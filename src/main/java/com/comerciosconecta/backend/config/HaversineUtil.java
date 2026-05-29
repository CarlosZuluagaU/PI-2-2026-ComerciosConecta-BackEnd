package com.comerciosconecta.backend.config;

/**
 * Calcula la distancia en kilómetros entre dos puntos geográficos
 * usando la fórmula de Haversine (sin necesidad de API externa).
 */
public class HaversineUtil {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private HaversineUtil() {}

    /**
     * @param lat1 Latitud del punto de origen (comercio)
     * @param lng1 Longitud del punto de origen (comercio)
     * @param lat2 Latitud del punto destino (cliente)
     * @param lng2 Longitud del punto destino (cliente)
     * @return Distancia en kilómetros
     */
    public static double calcularDistanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
