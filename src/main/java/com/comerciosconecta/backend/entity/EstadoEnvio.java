package com.comerciosconecta.backend.entity;

public enum EstadoEnvio {
    PENDIENTE,      // Orden pagada, aún no preparada
    PREPARANDO,     // Comercio está alistando el pedido
    DESPACHADO,     // Salió del comercio, guía generada
    EN_TRANSITO,    // En camino al destino
    ENTREGADO,      // Entregado al cliente
    FALLIDO,        // Intento de entrega fallido
    CANCELADO       // Envío cancelado
}
