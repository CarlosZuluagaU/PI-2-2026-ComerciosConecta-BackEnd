-- Crear tabla compras si no existe (por si Hibernate aún no la ha creado)
CREATE TABLE IF NOT EXISTS compras (
    id             bigserial    PRIMARY KEY,
    numero_factura varchar(255) NOT NULL UNIQUE,
    proveedor_id   bigint,
    fecha_compra   date,
    subtotal       float8,
    iva            int,
    total          float8,
    estado         varchar(255)
);

-- Añadir comercio_id (IF NOT EXISTS por si ya existía la tabla con la columna)
ALTER TABLE compras ADD COLUMN IF NOT EXISTS comercio_id INT REFERENCES comercio(id);
