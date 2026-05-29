package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.*;
import com.comerciosconecta.backend.entity.*;
import com.comerciosconecta.backend.repository.OrderRepository;
import com.comerciosconecta.backend.service.EnvioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/envios")
public class EnvioController {

    private final EnvioService envioService;
    private final OrderRepository orderRepository;

    public EnvioController(EnvioService envioService, OrderRepository orderRepository) {
        this.envioService = envioService;
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // CONFIGURACIÓN DEL COMERCIO
    // GET  /api/envios/configuracion/{comercioId}
    // PUT  /api/envios/configuracion/{comercioId}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/configuracion/{comercioId}")
    public ResponseEntity<ConfiguracionEnvio> obtenerConfiguracion(@PathVariable Integer comercioId) {
        return ResponseEntity.ok(envioService.obtenerConfiguracion(comercioId));
    }

    @PutMapping("/configuracion/{comercioId}")
    public ResponseEntity<ConfiguracionEnvio> guardarConfiguracion(
            @PathVariable Integer comercioId,
            @RequestBody ConfiguracionEnvio config) {
        return ResponseEntity.ok(envioService.guardarConfiguracion(comercioId, config));
    }

    // ─────────────────────────────────────────────────────────────
    // CREAR ENVÍO MANUAL para una orden existente (admin)
    // POST /api/envios/crear-manual
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/crear-manual")
    public ResponseEntity<?> crearManual(@RequestBody Map<String, Object> body) {
        try {
            Long orderId       = Long.valueOf(body.get("orderId").toString());
            Integer comercioId = Integer.valueOf(body.get("comercioId").toString());
            String tipoEnvio   = body.get("tipoEnvio").toString();
            String direccion   = body.containsKey("direccionDestino") ? body.get("direccionDestino").toString() : null;
            String ciudad      = body.containsKey("ciudadDestino")    ? body.get("ciudadDestino").toString()    : null;
            String depto       = body.containsKey("departamentoDestino") ? body.get("departamentoDestino").toString() : null;

            // If an envío already exists for this order, return it instead of failing
            try {
                Envio existing = envioService.obtenerPorOrder(orderId);
                return ResponseEntity.ok(Map.of(
                        "id",          existing.getId(),
                        "orderId",     existing.getOrderId(),
                        "tipoEnvio",   existing.getTipoEnvio(),
                        "estadoEnvio", existing.getEstadoEnvio()
                ));
            } catch (Exception ignored) { /* not found — proceed to create */ }

            Envio envio = envioService.crearEnvio(orderId, comercioId, tipoEnvio,
                    direccion, ciudad, depto, null, null, 0L, 0L);

            return ResponseEntity.ok(Map.of(
                    "id",          envio.getId(),
                    "orderId",     envio.getOrderId(),
                    "tipoEnvio",   envio.getTipoEnvio(),
                    "estadoEnvio", envio.getEstadoEnvio()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CALCULAR OPCIONES (público — usado en checkout)
    // POST /api/envios/calcular
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/calcular")
    public ResponseEntity<?> calcularOpciones(@RequestBody CalcularEnvioRequest req) {
        try {
            List<OpcionEnvioDto> opciones = envioService.calcularOpciones(req);
            return ResponseEntity.ok(opciones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LISTAR ENVÍOS DEL COMERCIO (admin)
    // GET /api/envios?comercioId=1&estado=PENDIENTE
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam Integer comercioId,
            @RequestParam(required = false) String estado) {
        List<Envio> envios = envioService.listarPorComercio(comercioId, estado);
        List<java.util.Map<String, Object>> result = envios.stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",                   e.getId());
            m.put("orderId",              e.getOrderId());
            m.put("comercioId",           e.getComercioId());
            m.put("tipoEnvio",            e.getTipoEnvio().name());
            m.put("estadoEnvio",          e.getEstadoEnvio().name());
            m.put("direccionDestino",     e.getDireccionDestino());
            m.put("ciudadDestino",        e.getCiudadDestino());
            m.put("departamentoDestino",  e.getDepartamentoDestino());
            m.put("distanciaKm",          e.getDistanciaKm());
            m.put("costoEnvioFinal",      e.getCostoEnvioFinal());
            m.put("costoEnvioCalculado",  e.getCostoEnvioCalculado());
            m.put("envioGratis",          e.getEnvioGratis());
            m.put("numeroGuia",           e.getNumeroGuia());
            m.put("nombreTransportadora", e.getNombreTransportadora());
            m.put("urlSeguimiento",       e.getUrlSeguimiento());
            m.put("nombreEntregador",     e.getNombreEntregador());
            m.put("telefonoEntregador",   e.getTelefonoEntregador());
            m.put("notasComercio",        e.getNotasComercio());
            m.put("notasCliente",         e.getNotasCliente());
            m.put("fechaCreacion",        e.getFechaCreacion());
            m.put("fechaDespacho",        e.getFechaDespacho());
            m.put("fechaEntregaEstimada", e.getFechaEntregaEstimada());
            orderRepository.findById(e.getOrderId()).ifPresent(order ->
                    m.put("comercioOrderNumber", order.getComercioOrderNumber()));
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────
    // VER ENVÍO DE UNA ORDEN (admin)
    // GET /api/envios/orden/{orderId}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/orden/{orderId}")
    public ResponseEntity<?> obtenerPorOrden(@PathVariable Long orderId) {
        try {
            Envio e = envioService.obtenerPorOrder(orderId);
            java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("id",                   e.getId());
            r.put("orderId",              e.getOrderId());
            r.put("comercioId",           e.getComercioId());
            r.put("tipoEnvio",            e.getTipoEnvio().name());
            r.put("estadoEnvio",          e.getEstadoEnvio().name());
            r.put("direccionDestino",     e.getDireccionDestino());
            r.put("ciudadDestino",        e.getCiudadDestino());
            r.put("departamentoDestino",  e.getDepartamentoDestino());
            r.put("costoEnvioFinal",      e.getCostoEnvioFinal());
            r.put("costoEnvioCalculado",  e.getCostoEnvioCalculado());
            r.put("envioGratis",          e.getEnvioGratis());
            r.put("numeroGuia",           e.getNumeroGuia());
            r.put("nombreTransportadora", e.getNombreTransportadora());
            r.put("urlSeguimiento",       e.getUrlSeguimiento());
            r.put("nombreEntregador",     e.getNombreEntregador());
            r.put("telefonoEntregador",   e.getTelefonoEntregador());
            r.put("notasComercio",        e.getNotasComercio());
            r.put("notasCliente",         e.getNotasCliente());
            return ResponseEntity.ok(r);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DESPACHAR (admin — comercio marca el pedido como despachado)
    // POST /api/envios/despachar/{envioId}
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/despachar/{envioId}")
    public ResponseEntity<?> despachar(
            @PathVariable Long envioId,
            @RequestBody DespacharEnvioRequest req) {
        try {
            Envio envio = envioService.despachar(envioId, req);
            return ResponseEntity.ok(Map.of(
                    "envioId", envio.getId(),
                    "numeroGuia", envio.getNumeroGuia(),
                    "estado", envio.getEstadoEnvio(),
                    "fechaDespacho", envio.getFechaDespacho()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ACTUALIZAR ESTADO (admin — actualiza tracking)
    // PUT /api/envios/estado/{envioId}
    // ─────────────────────────────────────────────────────────────

    @PutMapping("/estado/{envioId}")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long envioId,
            @RequestBody ActualizarEstadoEnvioRequest req) {
        try {
            Envio envio = envioService.actualizarEstado(envioId, req);
            return ResponseEntity.ok(Map.of(
                    "envioId", envio.getId(),
                    "estado", envio.getEstadoEnvio()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEGUIMIENTO PÚBLICO por número de guía
    // GET /api/envios/seguimiento/{numeroGuia}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/seguimiento/{numeroGuia}")
    public ResponseEntity<?> seguimientoPorGuia(@PathVariable String numeroGuia) {
        try {
            Envio envio = envioService.obtenerPorGuia(numeroGuia);
            List<SeguimientoEnvio> historial = envioService.obtenerHistorial(envio.getId());
            return ResponseEntity.ok(Map.of(
                    "numeroGuia", envio.getNumeroGuia(),
                    "tipoEnvio", envio.getTipoEnvio(),
                    "estadoActual", envio.getEstadoEnvio(),
                    "ciudadDestino", envio.getCiudadDestino() != null ? envio.getCiudadDestino() : "",
                    "fechaEntregaEstimada", envio.getFechaEntregaEstimada() != null ? envio.getFechaEntregaEstimada() : "",
                    "fechaEntregaReal", envio.getFechaEntregaReal() != null ? envio.getFechaEntregaReal() : "",
                    "transportadora", envio.getNombreTransportadora() != null ? envio.getNombreTransportadora() : "",
                    "historial", historial
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEGUIMIENTO por orderId (para el panel admin / cliente)
    // GET /api/envios/seguimiento/orden/{orderId}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/seguimiento/orden/{orderId}")
    public ResponseEntity<?> seguimientoPorOrden(@PathVariable Long orderId) {
        try {
            Envio envio = envioService.obtenerPorOrder(orderId);
            List<SeguimientoEnvio> historial = envioService.obtenerHistorial(envio.getId());
            return ResponseEntity.ok(Map.of(
                    "envioId", envio.getId(),
                    "numeroGuia", envio.getNumeroGuia() != null ? envio.getNumeroGuia() : "",
                    "tipoEnvio", envio.getTipoEnvio(),
                    "estadoActual", envio.getEstadoEnvio(),
                    "costoEnvioFinal", envio.getCostoEnvioFinal(),
                    "envioGratis", envio.getEnvioGratis(),
                    "ciudadDestino", envio.getCiudadDestino() != null ? envio.getCiudadDestino() : "",
                    "fechaEntregaEstimada", envio.getFechaEntregaEstimada() != null ? envio.getFechaEntregaEstimada() : "",
                    "historial", historial
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
