-- Nuevas columnas de tarifas por zona geográfica
ALTER TABLE configuracion_envio
    ADD COLUMN IF NOT EXISTS precio_ciudad_centavos       BIGINT,
    ADD COLUMN IF NOT EXISTS precio_departamento_centavos BIGINT,
    ADD COLUMN IF NOT EXISTS precio_nacional_centavos     BIGINT,
    ADD COLUMN IF NOT EXISTS dias_entrega_ciudad          INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS dias_entrega_departamento    INT DEFAULT 3;
