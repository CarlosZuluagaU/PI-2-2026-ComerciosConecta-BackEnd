package com.comerciosconecta.backend.entity;

public enum TipoEnvio {
    RECOGIDA,                    // Cliente recoge en tienda (gratis, misma ciudad)
    TRANSPORTADORA_CIUDAD,       // Transportadora en la misma ciudad
    TRANSPORTADORA_DEPARTAMENTO, // Transportadora mismo departamento, distinta ciudad
    TRANSPORTADORA_NACIONAL,     // Transportadora envío nacional

    // Valores legacy — mantenidos para compatibilidad con registros anteriores
    LOCAL_PROPIO,
    LOCAL_TRANSPORTADORA,
    NACIONAL_TRANSPORTADORA
}
