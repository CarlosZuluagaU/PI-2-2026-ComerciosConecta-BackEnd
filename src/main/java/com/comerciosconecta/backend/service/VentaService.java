package com.comerciosconecta.backend.service;

import com.comerciosconecta.backend.dto.*;
import com.comerciosconecta.backend.entity.Comercio;
import com.comerciosconecta.backend.entity.InvoiceRecord;
import com.comerciosconecta.backend.entity.Venta;
import com.comerciosconecta.backend.entity.VentaItem;
import com.comerciosconecta.backend.repository.ComercioRepository;
import com.comerciosconecta.backend.repository.InvoiceRecordRepository;
import com.comerciosconecta.backend.repository.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final InvoiceRecordRepository invoiceRecordRepository;
    private final FactusClient factusClient;
    private final ComercioRepository comercioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VentaService(VentaRepository ventaRepository,
                        InvoiceRecordRepository invoiceRecordRepository,
                        FactusClient factusClient,
                        ComercioRepository comercioRepository) {
        this.ventaRepository = ventaRepository;
        this.invoiceRecordRepository = invoiceRecordRepository;
        this.factusClient = factusClient;
        this.comercioRepository = comercioRepository;
    }

    // =========================================================================================
    //                                CREAR VENTA LOCAL
    // =========================================================================================
    @Transactional
    public Venta crearVenta(Venta venta) {

        if (venta.getDvCliente() == null) {
            venta.setDvCliente("1");
        }
        venta.setUuid(UUID.randomUUID().toString());
        venta.setEstado("CREATED");
        venta.setCreatedAt(LocalDateTime.now());
        venta.setUpdatedAt(LocalDateTime.now());

        if (venta.getItems() != null) {
            venta.getItems().forEach(i -> i.setVenta(venta));
        }

        double totalCalc = venta.getItems() != null ?
                venta.getItems().stream()
                        .mapToDouble(i -> {
                            double p = i.getPrecioTotal() != null ? i.getPrecioTotal() : 0.0;
                            int qty = i.getCantidad() != null ? i.getCantidad() : 1;
                            return p * qty;
                        })
                        .sum() : 0.0;

        venta.setTotalFactura(totalCalc);

        return ventaRepository.save(venta);
    }

    // =========================================================================================
    //                                   FACTURAR
    // =========================================================================================
    @Transactional
    public InvoiceRecord facturarVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        // Obtener numbering_range_id activo desde Factus
        String numberingRangeIdStr = factusClient.getFirstNumberingRangeId();
        if (numberingRangeIdStr == null)
            throw new RuntimeException("Factus no devolvió numbering_range_id activo");

        Integer numberingRangeId;
        try {
            numberingRangeId = Integer.valueOf(numberingRangeIdStr);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("numbering_range_id inválido desde Factus: " + numberingRangeIdStr);
        }

        // ================= ESTABLISHMENT DTO (desde Comercio del usuario) =================
        String estNombre   = "ComerciosConecta";
        String estDir      = "calle 10 # 3-13";
        String estTel      = "0987654321";
        String estEmail    = "comerciosconecta@gmail.com";
        Integer estMuniId  = 980;

        if (venta.getComercioId() != null) {
            comercioRepository.findById(Long.valueOf(venta.getComercioId())).ifPresent(c -> {
                // los campos se setean en la venta para que el lambda pueda accederlos
                if (c.getNombre()    != null) venta.setEstablecimientoNombre(c.getNombre());
                if (c.getDireccion() != null) venta.setEstablecimientoDireccion(c.getDireccion());
                if (c.getTelefono()  != null) venta.setEstablecimientoTelefono(c.getTelefono());
                if (c.getEmail()     != null) venta.setEstablecimientoEmail(c.getEmail());
            });
        }

        FactusEstablishmentDto establishmentDto = new FactusEstablishmentDto(
                venta.getEstablecimientoNombre()   != null ? venta.getEstablecimientoNombre()   : estNombre,
                venta.getEstablecimientoDireccion()!= null ? venta.getEstablecimientoDireccion(): estDir,
                venta.getEstablecimientoTelefono() != null ? venta.getEstablecimientoTelefono() : estTel,
                venta.getEstablecimientoEmail()    != null ? venta.getEstablecimientoEmail()    : estEmail,
                venta.getEstablecimientoMunicipioId() != null ? venta.getEstablecimientoMunicipioId() : estMuniId
        );

        // ================= CLIENTE DTO =================
        FactusCustomerDto customerDto = new FactusCustomerDto();
        String docNum = venta.getNumeroDocumentoCliente();
        // 222222222222 is 12 digits (consumidor final NIT). Real CC is 8-10 digits.
        boolean isNit = docNum != null && docNum.length() > 10;
        customerDto.setIdentification(docNum);
        customerDto.setDv(calcularDigitoVerificacion(docNum));
        customerDto.setCompany("");
        customerDto.setTrade_name("");
        customerDto.setNames(venta.getNombreCliente());
        customerDto.setAddress(venta.getDireccionCliente() != null && !venta.getDireccionCliente().isBlank()
                ? venta.getDireccionCliente() : "Sin dirección");
        customerDto.setEmail(venta.getEmailCliente());
        customerDto.setPhone(venta.getTelefonoCliente());
        customerDto.setLegal_organization_id(1); // 1=Persona Natural (B2C + consumidor final)
        customerDto.setTribute_id(21); // 21=No Responsable IVA
        customerDto.setIdentification_document_id(isNit ? 6 : 13); // 6=NIT, 13=CC
        customerDto.setMunicipality_id(980);

        // ================= ÍTEMS DTO =================
        List<FactusItemDto> itemsDto = (venta.getItems() == null)
                ? new ArrayList<>()
                : venta.getItems().stream().map(item -> {

            // Calcular precio unitario sin impuestos
            double precioUnitario = item.getPrecioSinImpuestos() != null ?
                    item.getPrecioSinImpuestos() : 0.0;

            // Calcular tasa de impuesto como string
            String taxRate = item.getPorcentajeIva() != null ?
                    String.format("%.2f", item.getPorcentajeIva()) : "0.00";

            // Crear lista de withholding taxes vacía por defecto
            List<FactusWithholdingTaxDto> withholdingTaxes = new ArrayList<>();

            // 999 was an old invalid fallback; treat it as "not set" and use 1 (UNSPSC)
            int stdCode = (item.getStandardCodeId() != null && item.getStandardCodeId() != 999)
                    ? item.getStandardCodeId() : 1;
            int unitMeasure = item.getUnidadMedidaId() != null ? item.getUnidadMedidaId() : 70;
            int tributeId   = item.getTributeId()     != null ? item.getTributeId()     : 1;
            double discount = item.getDescuentoRate() != null ? item.getDescuentoRate() : 0.0;
            int isExcluded  = item.getIsExcluded()    != null ? item.getIsExcluded()    : 0;

            return new FactusItemDto(
                    item.getCodigoProducto(),
                    item.getNombre(),
                    item.getCantidad(),
                    discount,
                    precioUnitario,
                    taxRate,
                    unitMeasure,
                    stdCode,
                    isExcluded,
                    tributeId,
                    withholdingTaxes
            );
        }).collect(Collectors.toList());

        // ================= ALLOWANCE CHARGES =================
        List<FactusAllowanceChargeDto> allowanceCharges = new ArrayList<>();


        // ================= REQUEST FINAL =================
        FactusBillRequest factusRequest = new FactusBillRequest();
        factusRequest.setDocument("01"); // Factura de venta
        factusRequest.setNumbering_range_id(numberingRangeId);
        factusRequest.setReference_code("F" + ventaId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        factusRequest.setObservation(venta.getNota() != null ? venta.getNota() : "");
        factusRequest.setPayment_method_code(10); // Código método de pago
        factusRequest.setEstablishment(establishmentDto);
        factusRequest.setCustomer(customerDto);
        factusRequest.setItems(itemsDto);
        factusRequest.setAllowance_charges(allowanceCharges);

        // ================= ENVIAR A FACTUS =================
        FactusValidationResponse resp = factusClient.crearFactura(factusRequest);

        // ================= GUARDAR REGISTRO =================
        InvoiceRecord record = new InvoiceRecord();
        record.setVentaId(ventaId);

        try {
            record.setRawResponse(resp != null ? objectMapper.writeValueAsString(resp) : "NULL RESPONSE");
        } catch (Exception e) {
            record.setRawResponse(resp != null ? resp.toString() : "NULL RESPONSE");
        }

        // Factus returns status as integer 201 in the JSON body on success.
        // Consider success when: no explicit errors AND data is present.
        boolean hasExplicitErrors = resp != null && resp.getErrors() != null && !resp.getErrors().isEmpty();
        boolean hasData = resp != null && resp.getData() != null;
        boolean success = hasData && !hasExplicitErrors;

        if (success) {
            record.setStatus("INVOICED");
            venta.setEstado("INVOICED");

            Map<String, Object> data = resp.getData();
            String billId = extractBillId(data);
            String number = extractBillNumber(data);
            if (billId != null) record.setFactusBillId(billId);
            if (number != null) record.setNumber(number);
        } else {
            record.setStatus("ERROR");
            venta.setEstado("ERROR");
        }

        invoiceRecordRepository.save(record);
        venta.setUpdatedAt(LocalDateTime.now());
        ventaRepository.save(venta);

        return record;
    }

    // Método auxiliar para calcular dígito de verificación (para NIT colombiano)
    private String calcularDigitoVerificacion(String nit) {
        if (nit == null || nit.length() < 8) return "0";

        try {
            int[] factores = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
            int suma = 0;

            for (int i = 0; i < nit.length(); i++) {
                int digito = Character.getNumericValue(nit.charAt(nit.length() - 1 - i));
                suma += digito * factores[i];
            }

            int resto = suma % 11;
            if (resto == 0 || resto == 1) {
                return String.valueOf(resto);
            } else {
                return String.valueOf(11 - resto);
            }
        } catch (Exception e) {
            return "0";
        }
    }

    // Métodos auxiliares para extraer información de la respuesta
    @SuppressWarnings("unchecked")
    private String extractBillId(Map<String, Object> data) {
        if (data.containsKey("bill_id")) return data.get("bill_id").toString();
        // Factus nests it under data.bill.id
        if (data.get("bill") instanceof Map) {
            Map<String, Object> bill = (Map<String, Object>) data.get("bill");
            if (bill.containsKey("id")) return bill.get("id").toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractBillNumber(Map<String, Object> data) {
        if (data.containsKey("number")) return data.get("number").toString();
        // Factus nests it under data.bill.number
        if (data.get("bill") instanceof Map) {
            Map<String, Object> bill = (Map<String, Object>) data.get("bill");
            if (bill.containsKey("number")) return bill.get("number").toString();
        }
        return null;
    }

    // En tu VentaService.java, agrega este método:

    public VentaDTO mapToVentaDTO(Venta venta) {
        if (venta == null) {
            return null;
        }

        // Mapear items
        List<VentaItemDTO> itemDTOs = venta.getItems() == null ?
                java.util.Collections.emptyList() :
                venta.getItems().stream()
                        .map(item -> new VentaItemDTO(
                                item.getId(),
                                item.getCodigoProducto(),
                                item.getNombre(),
                                item.getCantidad(),
                                item.getPrecioTotal(),
                                item.getPorcentajeIva()
                        ))
                        .collect(java.util.stream.Collectors.toList());

        return new VentaDTO(
                venta.getId(),
                venta.getUuid(),
                venta.getEstado(),
                venta.getTotalFactura(),
                venta.getNota(),
                venta.getReferencia(),
                venta.getNombreCliente(),
                venta.getNumeroDocumentoCliente(),
                venta.getCreatedAt(),
                itemDTOs
        );
    }
}