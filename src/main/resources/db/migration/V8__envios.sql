-- ─────────────────────────────────────────────────────────────
-- V8: Sistema de envíos
-- ─────────────────────────────────────────────────────────────

-- 1. Configuración de envío por comercio
CREATE TABLE IF NOT EXISTS configuracion_envio (
    id                              BIGSERIAL PRIMARY KEY,
    comercio_id                     INTEGER NOT NULL UNIQUE,
    latitud                         DOUBLE PRECISION,
    longitud                        DOUBLE PRECISION,
    radio_local_km                  DOUBLE PRECISION DEFAULT 25.0,
    permitir_recogida               BOOLEAN DEFAULT TRUE,
    permitir_entrega_local          BOOLEAN DEFAULT TRUE,
    permitir_entrega_nacional       BOOLEAN DEFAULT FALSE,
    tipo_entrega_local              VARCHAR(20) DEFAULT 'PROPIO',
    tarifa_local_propio_fija        BIGINT,
    tarifa_local_propio_por_km      BIGINT,
    tarifa_local_transport_fija     BIGINT,
    tarifa_local_transport_por_km   BIGINT,
    tarifa_nacional_fija            BIGINT,
    tarifa_nacional_por_km          BIGINT,
    monto_minimo_envio_gratis       BIGINT,
    dias_entrega_local_propio       INTEGER DEFAULT 1,
    dias_entrega_local_transport    INTEGER DEFAULT 2,
    dias_entrega_nacional           INTEGER DEFAULT 5,
    nombre_transportadora           VARCHAR(100)
);

-- 2. Envío por orden
CREATE TABLE IF NOT EXISTS envio (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL UNIQUE,
    comercio_id             INTEGER NOT NULL,
    tipo_envio              VARCHAR(30) NOT NULL,
    estado_envio            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    direccion_destino       TEXT,
    ciudad_destino          VARCHAR(100),
    departamento_destino    VARCHAR(100),
    lat_destino             DOUBLE PRECISION,
    lng_destino             DOUBLE PRECISION,
    distancia_km            DOUBLE PRECISION,
    costo_envio_calculado   BIGINT DEFAULT 0,
    costo_envio_final       BIGINT DEFAULT 0,
    envio_gratis            BOOLEAN DEFAULT FALSE,
    fecha_creacion          TIMESTAMP,
    fecha_despacho          TIMESTAMP,
    fecha_entrega_estimada  DATE,
    fecha_entrega_real      DATE,
    numero_guia             VARCHAR(100) UNIQUE,
    nombre_entregador       VARCHAR(150),
    telefono_entregador     VARCHAR(30),
    url_seguimiento         TEXT,
    nombre_transportadora   VARCHAR(100),
    notas_comercio          TEXT,
    notas_cliente           TEXT
);

-- 3. Historial de seguimiento
CREATE TABLE IF NOT EXISTS seguimiento_envio (
    id               BIGSERIAL PRIMARY KEY,
    envio_id         BIGINT NOT NULL REFERENCES envio(id) ON DELETE CASCADE,
    estado           VARCHAR(20) NOT NULL,
    descripcion      TEXT,
    ubicacion_actual VARCHAR(200),
    fecha            TIMESTAMP NOT NULL
);

-- 4. Columnas de envío en la tabla orders (si no existen)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tipo_envio              VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_cost_in_cents  BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS direccion_destino        TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ciudad_destino           VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS departamento_destino     VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS lat_destino              DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS lng_destino              DOUBLE PRECISION;

-- Índices
CREATE INDEX IF NOT EXISTS idx_envio_comercio_id  ON envio(comercio_id);
CREATE INDEX IF NOT EXISTS idx_envio_estado       ON envio(estado_envio);
CREATE INDEX IF NOT EXISTS idx_seguimiento_envio  ON seguimiento_envio(envio_id);
