ALTER TABLE compras ADD COLUMN IF NOT EXISTS comercio_id INT REFERENCES comercio(id);
