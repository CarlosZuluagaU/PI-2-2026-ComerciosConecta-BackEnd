-- V9: Add address fields to configuracion_envio (detected via reverse geocoding)
ALTER TABLE configuracion_envio
    ADD COLUMN IF NOT EXISTS direccion_comercio   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ciudad_comercio      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS departamento_comercio VARCHAR(100);
