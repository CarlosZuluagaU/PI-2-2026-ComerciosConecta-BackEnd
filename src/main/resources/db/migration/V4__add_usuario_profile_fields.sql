-- Add profile fields to usuario table (safe: IF NOT EXISTS)
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS apellido         VARCHAR(255);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS telefono         VARCHAR(50);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS tipo_documento   VARCHAR(50);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS numero_documento VARCHAR(100);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_nacimiento VARCHAR(30);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ciudad           VARCHAR(100);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS direccion        VARCHAR(255);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS biografia        VARCHAR(500);

-- Add ciudad to comercio table (safe: IF NOT EXISTS)
ALTER TABLE comercio ADD COLUMN IF NOT EXISTS ciudad          VARCHAR(100);
