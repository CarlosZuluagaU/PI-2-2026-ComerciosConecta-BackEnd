package com.comerciosconecta.backend.service;

import com.comerciosconecta.backend.config.HaversineUtil;
import com.comerciosconecta.backend.dto.*;
import com.comerciosconecta.backend.entity.*;
import com.comerciosconecta.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnvioService {

    private static final Logger log = LoggerFactory.getLogger(EnvioService.class);

    private final EnvioRepository envioRepository;
    private final SeguimientoEnvioRepository seguimientoRepository;
    private final ConfiguracionEnvioRepository configuracionRepository;
    private final OrderRepository orderRepository;

    public EnvioService(EnvioRepository envioRepository,
                        SeguimientoEnvioRepository seguimientoRepository,
                        ConfiguracionEnvioRepository configuracionRepository,
                        OrderRepository orderRepository) {
        this.envioRepository = envioRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.configuracionRepository = configuracionRepository;
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // CONFIGURACIÓN DEL COMERCIO
    // ─────────────────────────────────────────────────────────────

    public ConfiguracionEnvio obtenerConfiguracion(Integer comercioId) {
        return configuracionRepository.findByComercioId(comercioId)
                .orElseGet(() -> {
                    ConfiguracionEnvio nueva = new ConfiguracionEnvio();
                    nueva.setComercioId(comercioId);
                    return nueva;
                });
    }

    @Transactional
    public ConfiguracionEnvio guardarConfiguracion(Integer comercioId, ConfiguracionEnvio config) {
        ConfiguracionEnvio existente = configuracionRepository.findByComercioId(comercioId)
                .orElse(new ConfiguracionEnvio());
        existente.setComercioId(comercioId);
        existente.setLatitud(config.getLatitud());
        existente.setLongitud(config.getLongitud());
        existente.setDireccionComercio(config.getDireccionComercio());
        existente.setCiudadComercio(config.getCiudadComercio());
        existente.setDepartamentoComercio(config.getDepartamentoComercio());
        existente.setNombreTransportadora(config.getNombreTransportadora());
        existente.setMontoMinimoEnvioGratis(config.getMontoMinimoEnvioGratis());
        // Tarifas por zona
        existente.setPrecioCiudadCentavos(config.getPrecioCiudadCentavos());
        existente.setPrecioDepartamentoCentavos(config.getPrecioDepartamentoCentavos());
        existente.setPrecioNacionalCentavos(config.getPrecioNacionalCentavos());
        existente.setDiasEntregaCiudad(config.getDiasEntregaCiudad() != null ? config.getDiasEntregaCiudad() : 1);
        existente.setDiasEntregaDepartamento(config.getDiasEntregaDepartamento() != null ? config.getDiasEntregaDepartamento() : 3);
        existente.setDiasEntregaNacional(config.getDiasEntregaNacional() != null ? config.getDiasEntregaNacional() : 5);
        return configuracionRepository.save(existente);
    }

    // ─────────────────────────────────────────────────────────────
    // CÁLCULO DE OPCIONES DE ENVÍO (checkout)
    // ─────────────────────────────────────────────────────────────

    public List<OpcionEnvioDto> calcularOpciones(CalcularEnvioRequest req) {
        ConfiguracionEnvio cfg = configuracionRepository.findByComercioId(req.getComercioId())
                .orElseThrow(() -> new RuntimeException("El comercio no tiene configuración de envío"));

        Long montoOrden = req.getMontoOrdenEnCentavos() != null ? req.getMontoOrdenEnCentavos() : 0L;
        boolean envioGratisPorMonto = cfg.getMontoMinimoEnvioGratis() != null
                && montoOrden >= cfg.getMontoMinimoEnvioGratis();

        String zona = detectarZona(cfg, req);
        String transportadora = cfg.getNombreTransportadora();
        String nombreTransp = (transportadora != null && !transportadora.isBlank()) ? transportadora : "transportadora";

        List<OpcionEnvioDto> opciones = new ArrayList<>();

        if ("CIUDAD".equals(zona)) {
            // Recogida en tienda: solo disponible si el cliente está en la misma ciudad
            OpcionEnvioDto recogida = new OpcionEnvioDto();
            recogida.setTipoEnvio("RECOGIDA");
            recogida.setDescripcion("Recoger en tienda (gratis)");
            recogida.setCostoEnCentavos(0L);
            recogida.setCostoFinalEnCentavos(0L);
            recogida.setEnvioGratis(true);
            recogida.setDiasEstimados(0);
            opciones.add(recogida);

            // Transportadora en la misma ciudad
            long costo = cfg.getPrecioCiudadCentavos() != null ? cfg.getPrecioCiudadCentavos() : 0L;
            int dias = cfg.getDiasEntregaCiudad() != null ? cfg.getDiasEntregaCiudad() : 1;
            opciones.add(buildOpcion("TRANSPORTADORA_CIUDAD",
                    "Envío con " + nombreTransp + " en la ciudad",
                    costo, envioGratisPorMonto, dias, transportadora));

        } else if ("DEPARTAMENTO".equals(zona)) {
            long costo = cfg.getPrecioDepartamentoCentavos() != null ? cfg.getPrecioDepartamentoCentavos() : 0L;
            int dias = cfg.getDiasEntregaDepartamento() != null ? cfg.getDiasEntregaDepartamento() : 3;
            opciones.add(buildOpcion("TRANSPORTADORA_DEPARTAMENTO",
                    "Envío con " + nombreTransp + " en el departamento",
                    costo, envioGratisPorMonto, dias, transportadora));

        } else { // NACIONAL
            long costo = cfg.getPrecioNacionalCentavos() != null ? cfg.getPrecioNacionalCentavos() : 0L;
            int dias = cfg.getDiasEntregaNacional() != null ? cfg.getDiasEntregaNacional() : 5;
            opciones.add(buildOpcion("TRANSPORTADORA_NACIONAL",
                    "Envío nacional con " + nombreTransp,
                    costo, envioGratisPorMonto, dias, transportadora));
        }

        return opciones;
    }

    private String detectarZona(ConfiguracionEnvio cfg, CalcularEnvioRequest req) {
        String deptoComercio  = cfg.getDepartamentoComercio();
        String deptoDestino   = req.getDepartamentoDestino();
        String ciudadComercio = cfg.getCiudadComercio();
        String ciudadDestino  = req.getCiudadDestino();

        if (deptoComercio != null && !deptoComercio.isBlank()
                && deptoDestino != null && !deptoDestino.isBlank()) {
            boolean mismoDepto = normalizarUbicacion(deptoComercio).equals(normalizarUbicacion(deptoDestino));
            if (!mismoDepto) return "NACIONAL";

            // Mismo departamento — verificar ciudad
            if (ciudadComercio != null && !ciudadComercio.isBlank()
                    && ciudadDestino != null && !ciudadDestino.isBlank()) {
                String nc = normalizarUbicacion(ciudadComercio);
                String nd = normalizarUbicacion(ciudadDestino);
                boolean mismaCiudad = nc.equals(nd) || nc.startsWith(nd) || nd.startsWith(nc);
                return mismaCiudad ? "CIUDAD" : "DEPARTAMENTO";
            }
            return "DEPARTAMENTO"; // mismo departamento, sin info de ciudad
        }

        // Sin info de departamento → usar distancia si hay coordenadas
        if (cfg.getLatitud() != null && cfg.getLongitud() != null
                && req.getLatDestino() != null && req.getLngDestino() != null) {
            double distanciaKm = HaversineUtil.calcularDistanciaKm(
                    cfg.getLatitud(), cfg.getLongitud(),
                    req.getLatDestino(), req.getLngDestino());
            if (distanciaKm <= 15) return "CIUDAD";
            if (distanciaKm <= 100) return "DEPARTAMENTO";
        }

        return "NACIONAL"; // sin información suficiente → nacional como default seguro
    }

    /** Normaliza nombre de ciudad/departamento para comparación tolerante a variantes del geocodificador */
    private String normalizarUbicacion(String s) {
        return s.trim().toLowerCase()
                .replaceAll("\\s+ciudad$", "")
                .replaceAll("\\s+municipio$", "")
                .replaceAll("\\s+distrito$", "")
                .replaceAll(",.*", "")   // elimina todo después de una coma
                .trim();
    }

    private OpcionEnvioDto buildOpcion(String tipo, String descripcion, long costo,
                                       boolean envioGratisPorMonto, int dias, String transportadora) {
        OpcionEnvioDto op = new OpcionEnvioDto();
        op.setTipoEnvio(tipo);
        op.setDescripcion(descripcion);
        op.setCostoEnCentavos(costo);
        op.setCostoFinalEnCentavos(envioGratisPorMonto ? 0L : costo);
        op.setEnvioGratis(envioGratisPorMonto || costo == 0L);
        op.setDiasEstimados(dias);
        op.setTransportadora(transportadora);
        return op;
    }

    // ─────────────────────────────────────────────────────────────
    // CREAR ENVÍO al confirmar el pago (idempotente)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Envio crearEnvioSiNoExiste(Long orderId, Integer comercioId, String tipoEnvio,
                                      String direccionDestino, String ciudadDestino, String departamentoDestino,
                                      Double latDestino, Double lngDestino,
                                      Long costoEnvioFinal, Long montoOrden) {
        // Si ya existe un envío para esta orden, no crear otro
        if (envioRepository.findByOrderId(orderId).isPresent()) {
            return envioRepository.findByOrderId(orderId).get();
        }
        return crearEnvio(orderId, comercioId, tipoEnvio, direccionDestino, ciudadDestino,
                departamentoDestino, latDestino, lngDestino, costoEnvioFinal, montoOrden);
    }

    // ─────────────────────────────────────────────────────────────
    // CANCELAR ENVÍO cuando la orden es cancelada o falla el pago
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void cancelarEnvioPorOrden(Long orderId) {
        envioRepository.findByOrderId(orderId).ifPresent(envio -> {
            EstadoEnvio estado = envio.getEstadoEnvio();
            // Solo cancelar si el envío aún no fue despachado o entregado
            if (estado == EstadoEnvio.PENDIENTE || estado == EstadoEnvio.PREPARANDO) {
                envio.setEstadoEnvio(EstadoEnvio.CANCELADO);
                envioRepository.save(envio);
                SeguimientoEnvio evento = new SeguimientoEnvio(envio, EstadoEnvio.CANCELADO,
                        "Envío cancelado — pago no completado", null);
                seguimientoRepository.save(evento);
                log.info("Envío {} cancelado por fallo/cancelación de la orden {}", envio.getId(), orderId);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // CREAR ENVÍO (interno)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Envio crearEnvio(Long orderId, Integer comercioId, String tipoEnvio,
                             String direccionDestino, String ciudadDestino, String departamentoDestino,
                             Double latDestino, Double lngDestino,
                             Long costoEnvioFinal, Long montoOrden) {

        ConfiguracionEnvio cfg = configuracionRepository.findByComercioId(comercioId).orElse(null);

        Envio envio = new Envio();
        envio.setOrderId(orderId);
        envio.setComercioId(comercioId);
        envio.setTipoEnvio(TipoEnvio.valueOf(tipoEnvio));
        envio.setEstadoEnvio(EstadoEnvio.PENDIENTE);
        envio.setDireccionDestino(direccionDestino);
        envio.setCiudadDestino(ciudadDestino);
        envio.setDepartamentoDestino(departamentoDestino);
        envio.setLatDestino(latDestino);
        envio.setLngDestino(lngDestino);
        envio.setFechaCreacion(LocalDateTime.now());

        // Calcular distancia si hay coordenadas
        if (cfg != null && cfg.getLatitud() != null && cfg.getLongitud() != null
                && latDestino != null && lngDestino != null) {
            double distancia = HaversineUtil.calcularDistanciaKm(
                    cfg.getLatitud(), cfg.getLongitud(), latDestino, lngDestino);
            envio.setDistanciaKm(Math.round(distancia * 10.0) / 10.0);
        }

        // Costo
        boolean esGratis = TipoEnvio.RECOGIDA.name().equals(tipoEnvio) || costoEnvioFinal == 0L;
        envio.setCostoEnvioCalculado(costoEnvioFinal);
        envio.setCostoEnvioFinal(costoEnvioFinal);
        envio.setEnvioGratis(esGratis);

        Envio saved = envioRepository.save(envio);

        // Primer evento de seguimiento
        SeguimientoEnvio evento = new SeguimientoEnvio(saved, EstadoEnvio.PENDIENTE,
                "Pedido recibido, pendiente de preparación", null);
        seguimientoRepository.save(evento);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // PREPARANDO (cuando el pago es confirmado)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void marcarPreparando(Long orderId) {
        envioRepository.findByOrderId(orderId).ifPresent(envio -> {
            envio.setEstadoEnvio(EstadoEnvio.PREPARANDO);
            envioRepository.save(envio);
            SeguimientoEnvio evento = new SeguimientoEnvio(envio, EstadoEnvio.PREPARANDO,
                    "Pago confirmado — el comercio está alistando tu pedido", null);
            seguimientoRepository.save(evento);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // DESPACHAR
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Envio despachar(Long envioId, DespacharEnvioRequest req) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado: " + envioId));

        envio.setEstadoEnvio(EstadoEnvio.DESPACHADO);
        envio.setFechaDespacho(LocalDateTime.now());
        envio.setNombreEntregador(req.getNombreEntregador());
        envio.setTelefonoEntregador(req.getTelefonoEntregador());
        envio.setUrlSeguimiento(req.getUrlSeguimiento());
        envio.setFechaEntregaEstimada(req.getFechaEntregaEstimada());
        envio.setNotasComercio(req.getNotasComercio());

        if (req.getNombreTransportadora() != null) {
            envio.setNombreTransportadora(req.getNombreTransportadora());
        }

        // Número de guía: si la transportadora dio uno, usarlo; si no, generar interno
        String guia = req.getNumeroGuia();
        if (guia == null || guia.isBlank()) {
            guia = generarNumeroGuia(envio.getComercioId(), envio.getOrderId());
        }
        envio.setNumeroGuia(guia);

        Envio saved = envioRepository.save(envio);

        String descripcion = TipoEnvio.RECOGIDA == envio.getTipoEnvio()
                ? "Pedido listo para recoger en tienda"
                : "Pedido despachado — guía: " + guia;

        SeguimientoEnvio evento = new SeguimientoEnvio(saved, EstadoEnvio.DESPACHADO, descripcion, null);
        seguimientoRepository.save(evento);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // ACTUALIZAR ESTADO (tracking manual)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Envio actualizarEstado(Long envioId, ActualizarEstadoEnvioRequest req) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado: " + envioId));

        EstadoEnvio nuevoEstado = EstadoEnvio.valueOf(req.getEstadoEnvio());
        envio.setEstadoEnvio(nuevoEstado);

        if (nuevoEstado == EstadoEnvio.ENTREGADO) {
            envio.setFechaEntregaReal(LocalDate.now());
            orderRepository.findById(envio.getOrderId()).ifPresent(order -> {
                order.setStatus("COMPLETED");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("Orden {} marcada COMPLETED al registrar entrega del envío {}", order.getId(), envioId);
            });
        }

        envioRepository.save(envio);

        SeguimientoEnvio evento = new SeguimientoEnvio(envio, nuevoEstado,
                req.getDescripcion(), req.getUbicacionActual());
        seguimientoRepository.save(evento);

        return envio;
    }

    // ─────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────

    public Envio obtenerPorOrder(Long orderId) {
        return envioRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No se encontró envío para la orden: " + orderId));
    }

    public Envio obtenerPorGuia(String numeroGuia) {
        return envioRepository.findByNumeroGuia(numeroGuia)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada: " + numeroGuia));
    }

    public List<Envio> listarPorComercio(Integer comercioId, String estado) {
        if (estado != null && !estado.isBlank()) {
            return envioRepository.findByComercioIdAndEstadoEnvioOrderByFechaCreacionDesc(
                    comercioId, EstadoEnvio.valueOf(estado));
        }
        return envioRepository.findByComercioIdOrderByFechaCreacionDesc(comercioId);
    }

    public List<SeguimientoEnvio> obtenerHistorial(Long envioId) {
        return seguimientoRepository.findByEnvioIdOrderByFechaAsc(envioId);
    }

    // ─────────────────────────────────────────────────────────────
    // GENERACIÓN DE GUÍA INTERNA
    // ─────────────────────────────────────────────────────────────

    private String generarNumeroGuia(Integer comercioId, Long orderId) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "CC-" + comercioId + "-" + orderId + "-" + fecha;
    }
}
