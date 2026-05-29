package com.comerciosconecta.backend.service;

import com.comerciosconecta.backend.entity.*;
import com.comerciosconecta.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {
    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final EnvioService envioService;

    public CheckoutService(OrderRepository orderRepository,
                           PaymentRecordRepository paymentRecordRepository,
                           ProductoRepository productoRepository,
                           ClienteRepository clienteRepository,
                           VentaRepository ventaRepository,
                           EnvioService envioService) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.envioService = envioService;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order createOrder(String customerName, String customerEmail, String customerPhone,
                             String customerAddress, String customerCity,
                             List<OrderItem> items, Long totalInCents, Integer comercioId) {
        return createOrder(customerName, customerEmail, customerPhone, customerAddress, customerCity,
                items, totalInCents, comercioId, null, null, 0L, null, null, null, null, null);
    }

    @Transactional
    public Order createOrder(String customerName, String customerEmail, String customerPhone,
                             String customerAddress, String customerCity,
                             List<OrderItem> items, Long totalInCents, Integer comercioId,
                             String customerDocument,
                             String tipoEnvio, Long shippingCostInCents,
                             String direccionDestino, String ciudadDestino, String departamentoDestino,
                             Double latDestino, Double lngDestino) {
        Order o = new Order();
        o.setUuid(UUID.randomUUID().toString());
        o.setCustomerName(customerName);
        o.setCustomerEmail(customerEmail);
        o.setCustomerPhone(customerPhone);
        o.setCustomerAddress(customerAddress);
        o.setCustomerCity(customerCity);
        o.setCustomerDocument(customerDocument);
        o.setTotalInCents(totalInCents);
        o.setCurrency("COP");
        o.setStatus("CREATED");
        o.setComercioId(comercioId);
        long count = comercioId != null ? orderRepository.countByComercioId(comercioId) : orderRepository.count();
        o.setComercioOrderNumber((int) count + 1);
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());

        // Campos de envío
        o.setTipoEnvio(tipoEnvio);
        o.setShippingCostInCents(shippingCostInCents != null ? shippingCostInCents : 0L);
        o.setDireccionDestino(direccionDestino);
        o.setCiudadDestino(ciudadDestino);
        o.setDepartamentoDestino(departamentoDestino);
        o.setLatDestino(latDestino);
        o.setLngDestino(lngDestino);

        for (OrderItem item : items) {
            item.setOrder(o);
        }
        o.setItems(items);
        Order saved = orderRepository.save(o);

        // El envío se crea solo cuando el pago sea confirmado, no aquí.

        // Registrar o actualizar cliente automáticamente (sin duplicar por email ni documento)
        try {
            String docToStore = (customerDocument != null && !customerDocument.isBlank() && !customerDocument.contains("@"))
                    ? customerDocument : null;

            java.util.Optional<Cliente> byEmail = (customerEmail != null && !customerEmail.isBlank())
                ? (comercioId != null
                    ? clienteRepository.findByCorreoAndComercioId(customerEmail, comercioId)
                    : clienteRepository.findByCorreo(customerEmail))
                : java.util.Optional.empty();

            java.util.Optional<Cliente> byDoc = byEmail.isEmpty() && docToStore != null
                ? (comercioId != null
                    ? clienteRepository.findByNumeroDocumentoAndComercioId(docToStore, comercioId)
                    : clienteRepository.findByNumeroDocumento(docToStore))
                : java.util.Optional.empty();

            Cliente cliente = byEmail.or(() -> byDoc).orElse(null);

            if (cliente != null) {
                // Client already exists — do not overwrite admin-managed data.
                // The order is recorded with the checkout values; the Cliente record stays as-is.
            } else if (customerEmail != null && !customerEmail.isBlank()) {
                // New client
                Cliente nuevo = new Cliente();
                nuevo.setTipoDocumento("CC");
                nuevo.setNumeroDocumento(docToStore != null ? docToStore : "");
                String[] parts = customerName != null ? customerName.split(" ", 2) : new String[]{"", ""};
                nuevo.setNombres(parts[0]);
                nuevo.setApellidos(parts.length > 1 ? parts[1] : "");
                nuevo.setTelefono(customerPhone);
                nuevo.setCorreo(customerEmail);
                nuevo.setDireccion(customerAddress);
                nuevo.setCiudad(customerCity);
                nuevo.setEstado("Activo");
                nuevo.setComercioId(comercioId);
                clienteRepository.save(nuevo);
            }
        } catch (Exception ignored) {
            // No interrumpir el flujo si falla el registro del cliente
        }

        return saved;
    }

    public PaymentRecord savePaymentRecord(PaymentRecord p) {
        return paymentRecordRepository.save(p);
    }

    @Transactional
    public void markOrderPaidAndDecreaseStock(String wompiTransactionId, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            if (item.getProductoId() != null) {
                Producto producto = productoRepository.findById(item.getProductoId()).orElse(null);
                if (producto != null) {
                    int newStock = producto.getStock() - item.getCantidad();
                    producto.setStock(Math.max(newStock, 0));
                    productoRepository.save(producto);
                }
            }
        }

        // Crear el envío ahora que el pago está confirmado (solo si no existe ya)
        if (order.getTipoEnvio() != null && !order.getTipoEnvio().isBlank()
                && order.getComercioId() != null) {
            try {
                envioService.crearEnvioSiNoExiste(order.getId(), order.getComercioId(),
                        order.getTipoEnvio(), order.getDireccionDestino(),
                        order.getCiudadDestino(), order.getDepartamentoDestino(),
                        order.getLatDestino(), order.getLngDestino(),
                        order.getShippingCostInCents() != null ? order.getShippingCostInCents() : 0L,
                        order.getTotalInCents());
            } catch (Exception ignored) {}
        }
    }

    /** Descuenta stock y devuelve los productos que quedaron en mínimo o menos */
    @Transactional
    public List<Producto> decreaseStockForOrder(Order order) {
        List<Producto> lowStock = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Long productoId = item.getProductoId();
            if (productoId == null) continue;
            Producto p = productoRepository.findById(productoId).orElse(null);
            if (p == null) continue;
            int newStock = Math.max(p.getStock() - item.getCantidad(), 0);
            p.setStock(newStock);
            productoRepository.save(p);
            if (newStock <= p.getStockMinimo()) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    public void markOrderPendingPayment(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("PENDING_PAYMENT");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void markOrderFailed(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("FAILED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        envioService.cancelarEnvioPorOrden(orderId);
    }

    @Transactional
    public void restoreStockForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        for (OrderItem item : order.getItems()) {
            if (item.getProductoId() == null) continue;
            Producto producto = productoRepository.findById(item.getProductoId()).orElse(null);
            if (producto != null) {
                producto.setStock(producto.getStock() + item.getCantidad());
                productoRepository.save(producto);
            }
        }
    }

    @Transactional
    public Order cancelOrderByCustomer(String uuid, String customerEmail) {
        Order order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (!order.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("El email no coincide con el de la orden");
        }
        if (!"PAID".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Solo se pueden cancelar órdenes en estado Pagada");
        }

        // Restaurar stock de cada producto
        for (OrderItem item : order.getItems()) {
            if (item.getProductoId() == null) continue;
            Producto producto = productoRepository.findById(item.getProductoId()).orElse(null);
            if (producto == null) continue;
            producto.setStock(producto.getStock() + item.getCantidad());
            productoRepository.save(producto);
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        envioService.cancelarEnvioPorOrden(saved.getId());
        return saved;
    }

    @Transactional
    public Venta shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        // Marcar como enviada solo si aún no está en un estado posterior
        if (!"PROCESSING".equalsIgnoreCase(order.getStatus())
                && !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("PROCESSING");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        // Crear Venta a partir de la orden
        Venta venta = new Venta();
        venta.setComercioId(order.getComercioId());
        venta.setNombreCliente(order.getCustomerName());
        venta.setEmailCliente(order.getCustomerEmail());
        venta.setTelefonoCliente(order.getCustomerPhone());
        venta.setDireccionCliente(order.getCustomerAddress() != null ? order.getCustomerAddress() : "");
        // Use the customer's document collected at checkout; fall back to "consumidor final" NIT.
        String docNum = order.getCustomerDocument();
        if (docNum == null || docNum.isBlank() || docNum.contains("@")) docNum = "222222222222";
        venta.setTipoDocumentoCliente("CC");
        venta.setNumeroDocumentoCliente(docNum);
        venta.setDvCliente("1");
        venta.setNota("Pedido online ORD-" + order.getId());
        venta.setReferencia(order.getUuid());
        venta.setMetodoPago("Tienda Online");
        venta.setPaymentMethodCode(10);
        venta.setLegalOrganizationId(2);
        venta.setTributeId(21);
        venta.setIdentificationDocumentId(3);
        venta.setMunicipalityId(980);
        venta.setEstablecimientoNombre("ComerciosConecta");
        venta.setEstablecimientoDireccion("calle 10 # 3-13");
        venta.setEstablecimientoTelefono("0987654321");
        venta.setEstablecimientoEmail("comerciosconecta@gmail.com");
        venta.setEstablecimientoMunicipioId(980);

        double total = order.getTotalInCents() / 100.0;
        double subtotal = total / 1.19;
        double iva = total - subtotal;
        venta.setSubtotal(subtotal);
        venta.setTotalIva(iva);
        venta.setTotalFactura(total);
        venta.setEstado("CREATED");

        // Items
        List<VentaItem> ventaItems = new ArrayList<>();
        for (OrderItem oi : order.getItems()) {
            VentaItem vi = new VentaItem();
            vi.setNombre(oi.getNombre());
            vi.setCodigoProducto(String.valueOf(oi.getProductoId() != null ? oi.getProductoId() : 0));
            vi.setCantidad(oi.getCantidad());
            double precio = oi.getPriceInCents() / 100.0;
            vi.setPrecioTotal(precio);
            vi.setPrecioSinImpuestos(precio / 1.19);
            vi.setPorcentajeIva(19.0);
            vi.setDescuentoRate(0.0);
            vi.setUnidadMedidaId(70);
            vi.setStandardCodeId(1);
            vi.setIsExcluded(0);
            vi.setTributeId(1);
            vi.setVenta(venta);
            ventaItems.add(vi);
        }
        venta.setItems(ventaItems);

        return ventaRepository.save(venta);
    }
}
