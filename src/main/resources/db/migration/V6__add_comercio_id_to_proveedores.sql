ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS comercio_id INT REFERENCES comercio(id);
