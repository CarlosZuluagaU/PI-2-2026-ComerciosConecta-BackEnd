-- Crear tabla proveedores si no existe (por si Hibernate aún no la ha creado)
CREATE TABLE IF NOT EXISTS proveedores (
    id         bigserial    PRIMARY KEY,
    nombre     varchar(255) NOT NULL,
    contacto   varchar(255),
    telefono   varchar(255),
    email      varchar(255),
    direccion  varchar(255),
    tipo       varchar(255),
    estado     varchar(255)
);

-- Añadir comercio_id (IF NOT EXISTS por si ya existía la tabla con la columna)
ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS comercio_id INT REFERENCES comercio(id);
