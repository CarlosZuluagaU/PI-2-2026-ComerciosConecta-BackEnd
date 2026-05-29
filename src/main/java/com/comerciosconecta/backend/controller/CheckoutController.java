package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.entity.Order;
import com.comerciosconecta.backend.entity.OrderItem;
import com.comerciosconecta.backend.entity.PaymentRecord;
import com.comerciosconecta.backend.repository.OrderRepository;
import com.comerciosconecta.backend.service.CheckoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.comerciosconecta.backend.dto.CreateOrderRequest;
import com.comerciosconecta.backend.entity.SeguimientoEnvio;
import com.comerciosconecta.backend.entity.Venta;
import com.comerciosconecta.backend.entity.InvoiceRecord;
import com.comerciosconecta.backend.repository.EnvioRepository;
import com.comerciosconecta.backend.repository.InvoiceRecordRepository;
import com.comerciosconecta.backend.repository.SeguimientoEnvioRepository;
import com.comerciosconecta.backend.repository.VentaRepository;
import com.comerciosconecta.backend.service.EnvioService;
import com.comerciosconecta.backend.service.VentaService;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final VentaRepository ventaRepository;
    private final VentaService ventaService;
    private final InvoiceRecordRepository invoiceRecordRepository;
    private final EnvioRepository envioRepository;
    private final SeguimientoEnvioRepository seguimientoEnvioRepository;
    private final EnvioService envioService;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Value("${wompi.private-key}")
    private String wompiPrivateKey;

    @Value("${wompi.public-key}")
    private String wompiPublicKey;

    public CheckoutController(
            CheckoutService checkoutService,
            RestTemplate restTemplate,
            OrderRepository orderRepository,
            VentaRepository ventaRepository,
            VentaService ventaService,
            InvoiceRecordRepository invoiceRecordRepository,
            EnvioRepository envioRepository,
            SeguimientoEnvioRepository seguimientoEnvioRepository,
            EnvioService envioService) {
        this.checkoutService = checkoutService;
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
        this.ventaRepository = ventaRepository;
        this.ventaService = ventaService;
        this.invoiceRecordRepository = invoiceRecordRepository;
        this.envioRepository = envioRepository;
        this.seguimientoEnvioRepository = seguimientoEnvioRepository;
        this.envioService = envioService;
    }

    // 1) Crear orden local
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest req) {
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderRequest.Item i : req.getItems()) {
            OrderItem it = new OrderItem();
            it.setProductoId(i.getProductoId());
            it.setNombre(i.getNombre());
            it.setCantidad(i.getCantidad());
            it.setPriceInCents(i.getPriceInCents());
            it.setIvaPercentage(i.getIvaPercentage());
            it.setSubtotalInCents(i.getSubtotalInCents());
            items.add(it);
        }

        Order order = checkoutService.createOrder(
                req.getCustomerName(),
                req.getCustomerEmail(),
                req.getCustomerPhone(),
                req.getCustomerAddress(),
                req.getCustomerCity(),
                items,
                req.getTotalInCents(),
                req.getComercioId(),
                req.getCustomerDocument(),
                req.getTipoEnvio(),
                req.getShippingCostInCents(),
                req.getDireccionDestino(),
                req.getCiudadDestino(),
                req.getDepartamentoDestino(),
                req.getLatDestino(),
                req.getLngDestino()
        );

        Map<String, Object> resp = Map.of(
                "orderId", order.getId(),
                "orderUuid", order.getUuid(),
                "status", order.getStatus()
        );
        return ResponseEntity.ok(resp);
    }

    // 2) Crear Payment Link en Wompi
    @PostMapping("/create-payment-link/{orderId}")
    public ResponseEntity<Map<String, Object>> createPaymentLink(
            @PathVariable Long orderId,
            @RequestParam(required = false) String redirectUrl) {
        try {
        Order order = checkoutService.getOrderById(orderId);

        String finalRedirectUrl = (redirectUrl != null && !redirectUrl.isBlank())
                ? redirectUrl
                : "http://localhost:3000/store/order-confirmation";

        String url = "https://sandbox.wompi.co/v1/payment_links";
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Compra en Comercios Conecta - " + order.getUuid());
        body.put("description", "Compra de productos");
        body.put("single_use", true);
        body.put("collect_shipping", false);
        body.put("currency", order.getCurrency());
        body.put("amount_in_cents", order.getTotalInCents());
        body.put("redirect_url", finalRedirectUrl);
        body.put("reference", order.getUuid());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(wompiPrivateKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);

        Map respBody = resp.getBody();
        if (respBody == null || !respBody.containsKey("data")) {
            throw new RuntimeException("Wompi no retornó datos de pago. Respuesta: " + respBody);
        }
        Map data = (Map) respBody.get("data");
        if (data == null || data.get("id") == null) {
            throw new RuntimeException("Wompi retornó un link sin ID. Respuesta: " + respBody);
        }
        String linkId = (String) data.get("id");
        String paymentUrl = "https://checkout.wompi.co/l/" + linkId;

        // Marcar la orden como en espera de pago
        checkoutService.markOrderPendingPayment(order.getId());

        PaymentRecord p = new PaymentRecord();
        p.setOrderId(order.getId());
        p.setWompiTransactionId(null);
        p.setAmountInCents(order.getTotalInCents());
        p.setCurrency(order.getCurrency());
        p.setStatus("CREATED_LINK");
        p.setPaymentMethodType("PAYMENT_LINK");
        p.setRawResponse(resp.getBody().toString());

        checkoutService.savePaymentRecord(p);

        return ResponseEntity.ok(Map.of(
                "payment_url", paymentUrl,
                "orderId", order.getId(),
                "orderUuid", order.getUuid()
        ));
        } catch (Exception e) {
            logger.error("Error creando link de pago para orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al generar link de pago"));
        }
    }

    // 3) Iniciar pago SIN crear orden (orden se crea solo si Wompi aprueba)
    @PostMapping("/initiate-payment")
    public ResponseEntity<Map<String, Object>> initiatePayment(@RequestBody Map<String, Object> body) {
        try {
            String reference = java.util.UUID.randomUUID().toString();
            Long totalInCents = Long.valueOf(body.get("totalInCents").toString());
            String redirectUrl = body.get("redirectUrl") != null
                    ? body.get("redirectUrl").toString()
                    : "http://localhost:3000/store/order-confirmation";

            String url = "https://sandbox.wompi.co/v1/payment_links";
            Map<String, Object> wompiBody = new HashMap<>();
            wompiBody.put("name", "Compra en Comercios Conecta");
            wompiBody.put("description", "Compra de productos");
            wompiBody.put("single_use", true);
            wompiBody.put("collect_shipping", false);
            wompiBody.put("currency", "COP");
            wompiBody.put("amount_in_cents", totalInCents);
            wompiBody.put("redirect_url", redirectUrl);
            wompiBody.put("reference", reference);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(wompiPrivateKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(wompiBody, headers);
            ResponseEntity<Map<String, Object>> resp = restTemplate.postForEntity(url, entity, (Class<Map<String, Object>>)(Class<?>)Map.class);

            Map<String, Object> respBody = resp.getBody();
            Map<String, Object> data = respBody != null ? (Map<String, Object>) respBody.get("data") : null;
            String linkId = data != null ? (String) data.get("id") : null;
            if (linkId == null) throw new RuntimeException("Wompi no retornó link de pago");
            String paymentUrl = "https://checkout.wompi.co/l/" + linkId;

            return ResponseEntity.ok(Map.of("payment_url", paymentUrl, "reference", reference));
        } catch (Exception e) {
            logger.error("Error iniciando pago", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 3b) Confirmar orden DESPUÉS de pago aprobado por Wompi
    @PostMapping("/confirm-order")
    public ResponseEntity<Map<String, Object>> confirmOrder(@RequestBody CreateOrderRequest req) {
        try {
            List<OrderItem> items = new ArrayList<>();
            for (CreateOrderRequest.Item i : req.getItems()) {
                OrderItem it = new OrderItem();
                it.setProductoId(i.getProductoId());
                it.setNombre(i.getNombre());
                it.setCantidad(i.getCantidad());
                it.setPriceInCents(i.getPriceInCents());
                it.setIvaPercentage(i.getIvaPercentage());
                it.setSubtotalInCents(i.getSubtotalInCents());
                items.add(it);
            }

            Order order = checkoutService.createOrder(
                    req.getCustomerName(), req.getCustomerEmail(), req.getCustomerPhone(),
                    req.getCustomerAddress(), req.getCustomerCity(), items, req.getTotalInCents(),
                    req.getComercioId(),
                    req.getCustomerDocument(),
                    req.getTipoEnvio(), req.getShippingCostInCents(),
                    req.getDireccionDestino(), req.getCiudadDestino(), req.getDepartamentoDestino(),
                    req.getLatDestino(), req.getLngDestino()
            );
            // Marcar como PAID y descontar stock
            order.setStatus("PAID");
            orderRepository.save(order);
            List<com.comerciosconecta.backend.entity.Producto> lowStock =
                    checkoutService.decreaseStockForOrder(order);

            List<Map<String, Object>> lowStockAlerts = lowStock.stream().map(p -> {
                Map<String, Object> a = new java.util.HashMap<>();
                a.put("productoId", p.getId());
                a.put("nombre", p.getNombre());
                a.put("stock", p.getStock());
                a.put("stockMinimo", p.getStockMinimo());
                a.put("proveedor", p.getProveedor());
                return a;
            }).toList();

            Map<String, Object> resp = new java.util.HashMap<>();
            int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            resp.put("orderId", order.getId());
            resp.put("orderNumber", "ORD-" + String.format("%05d", num));
            resp.put("status", "PAID");
            resp.put("lowStockAlerts", lowStockAlerts);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error confirmando orden", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 4) Enviar orden → marca PROCESSING y crea Venta
    @PutMapping("/orders/{orderId}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(@PathVariable Long orderId) {
        try {
            com.comerciosconecta.backend.entity.Venta venta = checkoutService.shipOrder(orderId);
            return ResponseEntity.ok(Map.of(
                "message", "Pedido enviado correctamente",
                "ventaId", venta.getId(),
                "orderId", orderId
            ));
        } catch (Exception e) {
            logger.error("Error enviando orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 4) Cambiar estado de orden manualmente (para pruebas / admin)
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

            // Si se cancela, restaurar stock si ya estaba pagada y cancelar el envío
            if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                String curr = order.getStatus();
                if ("PAID".equalsIgnoreCase(curr)
                        || "PROCESSING".equalsIgnoreCase(curr)
                        || "COMPLETED".equalsIgnoreCase(curr)) {
                    checkoutService.restoreStockForOrder(orderId);
                }
                envioService.cancelarEnvioPorOrden(orderId);
            }

            order.setStatus(newStatus);
            order.setUpdatedAt(java.time.LocalDateTime.now());
            orderRepository.save(order);
            return ResponseEntity.ok(Map.of(
                "message", "Estado actualizado",
                "orderId", orderId,
                "status", newStatus
            ));
        } catch (Exception e) {
            logger.error("Error actualizando estado de orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 5) Facturar orden: ship (si no lo está) y luego invoicing Factus
    @PostMapping("/orders/{orderId}/facturar")
    public ResponseEntity<Map<String, Object>> facturarOrden(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

            // Solo se puede facturar si la orden ya fue enviada o entregada
            if (!"PROCESSING".equalsIgnoreCase(order.getStatus())
                    && !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "Solo se puede facturar una orden en estado Enviada o Entregada. Estado actual: "
                                + order.getStatus()));
            }

            // Buscar Venta existente; si no existe, crearla a partir de la orden ya enviada
            java.util.Optional<Venta> ventaExistente = ventaRepository.findByReferencia(order.getUuid());
            Venta venta;
            if (ventaExistente.isPresent()) {
                venta = ventaExistente.get();
            } else {
                venta = checkoutService.shipOrder(orderId);
            }

            // Facturar
            InvoiceRecord rec = ventaService.facturarVenta(venta.getId());

            if ("ERROR".equals(rec.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Error al facturar con Factus: " + rec.getRawResponse()));
            }

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("orderId", orderId);
            resp.put("ventaId", venta.getId());
            resp.put("invoiceId", rec.getId());
            resp.put("factusNumber", rec.getNumber());
            resp.put("status", rec.getStatus());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error facturando orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al facturar"));
        }
    }

    // 6) Estado de factura para una orden (¿ya fue facturada?)
    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<Map<String, Object>> getInvoiceForOrder(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

            java.util.Optional<Venta> ventaOpt = ventaRepository.findByReferencia(order.getUuid());
            if (ventaOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("invoiced", false));
            }

            Venta venta = ventaOpt.get();
            java.util.List<InvoiceRecord> invoices = invoiceRecordRepository.findByVentaIdOrderByCreatedAtDesc(venta.getId());
            if (invoices.isEmpty()) {
                return ResponseEntity.ok(Map.of("invoiced", false, "ventaId", venta.getId()));
            }

            InvoiceRecord latest = invoices.get(0);
            boolean invoiced = "INVOICED".equals(latest.getStatus());
            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("invoiced",      invoiced);
            resp.put("invoiceStatus", latest.getStatus() != null ? latest.getStatus() : "");
            resp.put("ventaId",       venta.getId());
            resp.put("factusNumber",  latest.getNumber()       != null ? latest.getNumber()       : "");
            resp.put("status",        latest.getStatus()       != null ? latest.getStatus()       : "");
            resp.put("factusBillId",  latest.getFactusBillId() != null ? latest.getFactusBillId() : "");
            resp.put("createdAt",     latest.getCreatedAt());
            resp.put("rawResponse",   latest.getRawResponse()  != null ? latest.getRawResponse()  : "");
            resp.put("customerName",  order.getCustomerName());
            resp.put("totalInCents",  order.getTotalInCents());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("invoiced", false));
        }
    }

    // 7a) Obtener una orden por ID numérico (usado por página de facturación admin)
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderByNumericId(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
            int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("id", order.getId());
            resp.put("comercioOrderNumber", num);
            resp.put("uuid", order.getUuid());
            resp.put("customerName", order.getCustomerName());
            resp.put("customerEmail", order.getCustomerEmail());
            resp.put("customerPhone", order.getCustomerPhone());
            resp.put("status", order.getStatus());
            resp.put("totalInCents", order.getTotalInCents());
            resp.put("totalInPesos", order.getTotalInCents() / 100.0);
            resp.put("createdAt", order.getCreatedAt());
            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("nombre", item.getNombre());
                    itemMap.put("cantidad", item.getCantidad());
                    itemMap.put("priceInCents", item.getPriceInCents());
                    itemMap.put("priceInPesos", item.getPriceInCents() / 100.0);
                    itemMap.put("subtotalInCents", item.getSubtotalInCents());
                    itemsList.add(itemMap);
                }
            }
            resp.put("items", itemsList);
            resp.put("itemsCount", itemsList.size());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Orden no encontrada"));
        }
    }

    // 7b) Obtener una orden por UUID (usado por página de confirmación del cliente)
    @GetMapping("/orders/by-uuid/{uuid}")
    public ResponseEntity<Map<String, Object>> getOrderByUuid(@PathVariable String uuid) {
        try {
            Order order = orderRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            resp.put("id", order.getId());
            resp.put("uuid", order.getUuid());
            resp.put("orderNumber", "ORD-" + String.format("%05d", num));
            resp.put("status", order.getStatus());
            resp.put("customerName", order.getCustomerName());
            resp.put("customerEmail", order.getCustomerEmail());
            resp.put("customerPhone", order.getCustomerPhone());
            resp.put("customerAddress", order.getCustomerAddress());
            resp.put("customerCity", order.getCustomerCity());
            resp.put("totalInCents", order.getTotalInCents());
            resp.put("totalInPesos", order.getTotalInCents() / 100.0);
            resp.put("currency", order.getCurrency());
            resp.put("tipoEnvio", order.getTipoEnvio());
            resp.put("shippingCostInCents", order.getShippingCostInCents());
            resp.put("createdAt", order.getCreatedAt());

            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("nombre", item.getNombre());
                    itemMap.put("cantidad", item.getCantidad());
                    itemMap.put("priceInCents", item.getPriceInCents());
                    itemMap.put("subtotalInCents", item.getSubtotalInCents());
                    itemsList.add(itemMap);
                }
            }
            resp.put("items", itemsList);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Orden no encontrada"));
        }
    }

    // 7c-bis) Marcar orden como fallida desde la página de confirmación (cuando el polling agota sin pago)
    @PostMapping("/orders/by-uuid/{uuid}/mark-failed")
    public ResponseEntity<Map<String, Object>> markOrderFailedByUuid(@PathVariable String uuid) {
        try {
            Order order = orderRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + uuid));

            String currentStatus = order.getStatus();

            if ("FAILED".equalsIgnoreCase(currentStatus) || "CANCELLED".equalsIgnoreCase(currentStatus)) {
                return ResponseEntity.ok(Map.of("status", currentStatus, "alreadyFailed", true));
            }

            // Solo marcar como fallida si aún está en estado previo al pago
            if (!"PENDING_PAYMENT".equalsIgnoreCase(currentStatus)
                    && !"CREATED".equalsIgnoreCase(currentStatus)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "La orden no puede marcarse como fallida en estado: " + currentStatus));
            }

            checkoutService.markOrderFailed(order.getId());
            logger.info("Orden {} marcada como FAILED desde confirmación (timeout polling)", uuid);
            return ResponseEntity.ok(Map.of("status", "FAILED", "alreadyFailed", false));
        } catch (Exception e) {
            logger.error("Error marcando orden {} como fallida", uuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al marcar como fallida"));
        }
    }

    // 7c) Confirmar pago desde la página de confirmación (idempotente)
    //     Llamado cuando Wompi redirige — verifica el estado real con la API de Wompi
    //     cuando se proporciona wompiTransactionId, para no depender del webhook.
    @PostMapping("/orders/by-uuid/{uuid}/confirm-payment")
    public ResponseEntity<Map<String, Object>> confirmPaymentByUuid(
            @PathVariable String uuid,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            Order order = orderRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + uuid));

            String currentStatus = order.getStatus();

            // Si ya está pagada o más avanzada, retornar éxito sin hacer nada (idempotente)
            if ("PAID".equalsIgnoreCase(currentStatus)
                    || "PROCESSING".equalsIgnoreCase(currentStatus)
                    || "COMPLETED".equalsIgnoreCase(currentStatus)) {
                int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
                return ResponseEntity.ok(Map.of(
                        "status", currentStatus,
                        "orderNumber", "ORD-" + String.format("%05d", num),
                        "alreadyPaid", true
                ));
            }

            // Solo confirmar si la orden está en estado previo al pago
            if (!"PENDING_PAYMENT".equalsIgnoreCase(currentStatus)
                    && !"CREATED".equalsIgnoreCase(currentStatus)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "La orden no puede ser confirmada en estado: " + currentStatus));
            }

            String wompiTxId = body != null ? body.getOrDefault("wompiTransactionId", "") : "";

            // Si tenemos el ID de transacción, verificar el estado real con la API de Wompi
            // antes de marcar como pagada (soluciona el caso sandbox donde el webhook no llega)
            if (wompiTxId != null && !wompiTxId.isBlank()) {
                try {
                    String wompiApiUrl = "https://sandbox.wompi.co/v1/transactions/" + wompiTxId;
                    HttpHeaders verifyHeaders = new HttpHeaders();
                    verifyHeaders.setBearerAuth(wompiPrivateKey);
                    org.springframework.http.HttpEntity<?> verifyEntity = new org.springframework.http.HttpEntity<>(verifyHeaders);
                    ResponseEntity<Map> verifyResp = restTemplate.exchange(
                            wompiApiUrl, org.springframework.http.HttpMethod.GET, verifyEntity, Map.class);
                    Map verifyBody = verifyResp.getBody();
                    if (verifyBody != null && verifyBody.get("data") != null) {
                        Map txData = (Map) verifyBody.get("data");
                        String txStatus = txData.get("status") != null ? txData.get("status").toString() : "";
                        if ("DECLINED".equalsIgnoreCase(txStatus)
                                || "ERROR".equalsIgnoreCase(txStatus)
                                || "VOIDED".equalsIgnoreCase(txStatus)) {
                            checkoutService.markOrderFailed(order.getId());
                            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                                    .body(Map.of("wompiStatus", txStatus, "error", "Pago no aprobado"));
                        }
                        if ("PENDING".equalsIgnoreCase(txStatus)) {
                            // Aún en proceso — el frontend debe reintentar
                            return ResponseEntity.status(HttpStatus.ACCEPTED)
                                    .body(Map.of("wompiStatus", "PENDING"));
                        }
                        // APPROVED — continuar a marcar como pagada
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo verificar transacción Wompi {}: {}", wompiTxId, e.getMessage());
                    // Si falla la verificación, continuar con el flujo normal (el webhook corregirá si aplica)
                }
            }

            // Marcar como PAID y descontar stock
            checkoutService.markOrderPaidAndDecreaseStock(wompiTxId, order.getId());
            try { envioService.marcarPreparando(order.getId()); } catch (Exception ignored) {}

            int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            return ResponseEntity.ok(Map.of(
                    "status", "PAID",
                    "orderNumber", "ORD-" + String.format("%05d", num),
                    "alreadyPaid", false
            ));
        } catch (Exception e) {
            logger.error("Error confirmando pago para orden {}", uuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al confirmar pago"));
        }
    }

    // 8) Buscar orden por email + número de orden o UUID (uso del cliente)
    @GetMapping("/orders/buscar")
    public ResponseEntity<Map<String, Object>> buscarOrdenCliente(
            @RequestParam String email,
            @RequestParam String orderNumber) {
        try {
            String emailTrim = email.trim();
            String ref       = orderNumber.trim();

            // Detectar si es UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
            boolean esUuid = ref.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

            Order order;
            if (esUuid) {
                order = orderRepository.findByEmailAndUuid(emailTrim, ref.toLowerCase())
                        .orElseThrow(() -> new RuntimeException("No se encontró una orden con ese email y referencia"));
            } else {
                // Parsear "ORD-00001", "ORD-1" o "1" → número entero
                String cleaned = ref.toUpperCase()
                        .replaceAll("^ORD-", "")   // quitar prefijo ORD-
                        .replaceAll("^0+", "");      // quitar ceros a la izquierda
                if (cleaned.isEmpty()) cleaned = "0";
                int num;
                try {
                    num = Integer.parseInt(cleaned);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Formato inválido. Usa el número de orden (ORD-00001) o la referencia UUID"));
                }
                order = orderRepository.findByEmailAndComercioOrderNumber(emailTrim, num)
                        .orElseThrow(() -> new RuntimeException("No se encontró una orden con ese email y número. Verifica que el email y el número de orden sean correctos"));
            }

            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            int orderNum = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            resp.put("uuid", order.getUuid());
            resp.put("orderNumber", "ORD-" + String.format("%05d", orderNum));
            resp.put("status", order.getStatus());
            resp.put("customerName", order.getCustomerName());
            resp.put("customerEmail", order.getCustomerEmail());
            resp.put("customerCity", order.getCustomerCity());
            resp.put("totalInCents", order.getTotalInCents());
            resp.put("totalInPesos", order.getTotalInCents() / 100.0);
            resp.put("tipoEnvio", order.getTipoEnvio());
            resp.put("createdAt", order.getCreatedAt());
            resp.put("canCancel", "PAID".equalsIgnoreCase(order.getStatus()));

            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("nombre", item.getNombre());
                    itemMap.put("cantidad", item.getCantidad());
                    itemMap.put("priceInCents", item.getPriceInCents());
                    itemMap.put("subtotalInCents", item.getSubtotalInCents());
                    itemsList.add(itemMap);
                }
            }
            resp.put("items", itemsList);

            // Incluir datos del envío si existe
            envioRepository.findByOrderId(order.getId()).ifPresent(envio -> {
                Map<String, Object> envioMap = new java.util.LinkedHashMap<>();
                envioMap.put("estadoEnvio", envio.getEstadoEnvio() != null ? envio.getEstadoEnvio().name() : null);
                envioMap.put("nombreTransportadora", envio.getNombreTransportadora());
                envioMap.put("numeroGuia", envio.getNumeroGuia());
                envioMap.put("fechaDespacho", envio.getFechaDespacho());
                envioMap.put("fechaEntregaEstimada", envio.getFechaEntregaEstimada());
                envioMap.put("fechaEntregaReal", envio.getFechaEntregaReal());
                envioMap.put("urlSeguimiento", envio.getUrlSeguimiento());
                envioMap.put("notasCliente", envio.getNotasCliente());

                List<Map<String, Object>> historial = new ArrayList<>();
                for (SeguimientoEnvio s : seguimientoEnvioRepository.findByEnvioIdOrderByFechaAsc(envio.getId())) {
                    Map<String, Object> ev = new java.util.LinkedHashMap<>();
                    ev.put("estado", s.getEstado() != null ? s.getEstado().name() : null);
                    ev.put("descripcion", s.getDescripcion());
                    ev.put("ubicacion", s.getUbicacionActual());
                    ev.put("fecha", s.getFecha());
                    historial.add(ev);
                }
                envioMap.put("historial", historial);
                resp.put("envio", envioMap);
            });

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Orden no encontrada"));
        }
    }

    // 9) Cancelar orden — acción del cliente (solo estado PAID)
    @PostMapping("/orders/{uuid}/cancelar")
    public ResponseEntity<Map<String, Object>> cancelarOrdenCliente(
            @PathVariable String uuid,
            @RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Se requiere el email"));
            }
            Order order = checkoutService.cancelOrderByCustomer(uuid, email);
            int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
            return ResponseEntity.ok(Map.of(
                    "message", "Orden cancelada exitosamente",
                    "orderNumber", "ORD-" + String.format("%05d", num),
                    "status", order.getStatus()
            ));
        } catch (Exception e) {
            logger.error("Error cancelando orden {} por cliente", uuid, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "No se pudo cancelar la orden"));
        }
    }

    // 7) Obtener todas las órdenes (filtradas por comercioId si se provee)
    @GetMapping("/all-orders")
    public ResponseEntity<List<Map<String, Object>>> getAllOrdersSimple(
            @RequestParam(required = false) Integer comercioId) {
        try {
            List<Order> orders = (comercioId != null)
                    ? orderRepository.findByComercioIdOrderByCreatedAtDesc(comercioId)
                    : orderRepository.findAllByOrderByCreatedAtDesc();

            List<Map<String, Object>> response = new ArrayList<>();
            for (Order order : orders) {
                Map<String, Object> orderMap = new HashMap<>();
                int num = order.getComercioOrderNumber() != null ? order.getComercioOrderNumber() : order.getId().intValue();
                orderMap.put("id", order.getId());
                orderMap.put("comercioOrderNumber", num);
                orderMap.put("uuid", order.getUuid());
                orderMap.put("customerName", order.getCustomerName());
                orderMap.put("customerEmail", order.getCustomerEmail());
                orderMap.put("customerPhone", order.getCustomerPhone());
                orderMap.put("status", order.getStatus());
                orderMap.put("totalInCents", order.getTotalInCents());
                orderMap.put("totalInPesos", order.getTotalInCents() / 100.0);
                orderMap.put("createdAt", order.getCreatedAt());
                orderMap.put("updatedAt", order.getUpdatedAt());

                // Items de la orden
                List<Map<String, Object>> itemsList = new ArrayList<>();
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productoId", item.getProductoId());
                        itemMap.put("nombre", item.getNombre());
                        itemMap.put("cantidad", item.getCantidad());
                        itemMap.put("priceInCents", item.getPriceInCents());
                        itemMap.put("priceInPesos", item.getPriceInCents() / 100.0);
                        itemMap.put("subtotalInCents", item.getSubtotalInCents());
                        itemsList.add(itemMap);
                    }
                }
                orderMap.put("items", itemsList);
                orderMap.put("itemsCount", itemsList.size());

                response.add(orderMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error obteniendo órdenes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}