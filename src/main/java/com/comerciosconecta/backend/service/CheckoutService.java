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

    public CheckoutService(OrderRepository orderRepository,
                           PaymentRecordRepository paymentRecordRepository,
                           ProductoRepository productoRepository,
                           ClienteRepository clienteRepository,
                           VentaRepository ventaRepository) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order createOrder(String customerName, String customerEmail, String customerPhone,
                             String customerAddress, String customerCity,
                             List<OrderItem> items, Long totalInCents, Integer comercioId) {
        Order o = new Order();
        o.setUuid(UUID.randomUUID().toString());
        o.setCustomerName(customerName);
        o.setCustomerEmail(customerEmail);
        o.setCustomerPhone(customerPhone);
        o.setCustomerAddress(customerAddress);
        o.setCustomerCity(customerCity);
        o.setTotalInCents(totalInCents);
        o.setCurrency("COP");
        o.setStatus("CREATED");
        o.setComercioId(comercioId);
        long count = comercioId != null ? orderRepository.countByComercioId(comercioId) : orderRepository.count();
        o.setComercioOrderNumber((int) count + 1);
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());

        for (OrderItem item : items) {
            item.setOrder(o);
        }
        o.setItems(items);
        Order saved = orderRepository.save(o);

        // Registrar cliente automáticamente si no existe para este comercio
        try {
            if (customerEmail != null && !customerEmail.isBlank()) {
                boolean exists = comercioId != null
                    ? clienteRepository.findByCorreoAndComercioId(customerEmail, comercioId).isPresent()
                    : clienteRepository.findByCorreo(customerEmail).isPresent();
                if (!exists) {
                    boolean docExists = comercioId != null
                        ? clienteRepository.findByNumeroDocumentoAndComercioId(customerEmail, comercioId).isPresent()
                        : clienteRepository.findByNumeroDocumento(customerEmail).isPresent();
                    if (!docExists) {
                        Cliente cliente = new Cliente();
                        cliente.setTipoDocumento("CC");
                        cliente.setNumeroDocumento(customerEmail);
                        String[] parts = customerName != null ? customerName.split(" ", 2) : new String[]{"", ""};
                        cliente.setNombres(parts[0]);
                        cliente.setApellidos(parts.length > 1 ? parts[1] : "");
                        cliente.setTelefono(customerPhone);
                        cliente.setCorreo(customerEmail);
                        cliente.setDireccion(customerAddress);
                        cliente.setCiudad(customerCity);
                        cliente.setEstado("Activo");
                        cliente.setComercioId(comercioId);
                        clienteRepository.save(cliente);
                    }
                }
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

    public void markOrderFailed(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("FAILED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public Venta shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        // Marcar como enviada
        order.setStatus("PROCESSING");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Crear Venta a partir de la orden
        Venta venta = new Venta();
        venta.setComercioId(order.getComercioId());
        venta.setNombreCliente(order.getCustomerName());
        venta.setEmailCliente(order.getCustomerEmail());
        venta.setTelefonoCliente(order.getCustomerPhone());
        venta.setDireccionCliente(order.getCustomerAddress() != null ? order.getCustomerAddress() : "");
        venta.setTipoDocumentoCliente("CC");
        venta.setNumeroDocumentoCliente(
            order.getCustomerEmail() != null ? order.getCustomerEmail() : order.getCustomerPhone()
        );
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
            vi.setStandardCodeId(999);
            vi.setIsExcluded(0);
            vi.setTributeId(1);
            vi.setVenta(venta);
            ventaItems.add(vi);
        }
        venta.setItems(ventaItems);

        return ventaRepository.save(venta);
    }
}
