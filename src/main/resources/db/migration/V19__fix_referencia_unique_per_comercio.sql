-- Drop the global unique constraint on referencia (same ref blocked across all comercios)
-- The constraint name is Hibernate-generated; try both the known name and a fallback.
DO $$ BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'productos' AND c.contype = 'u'
      AND c.conname = 'ukiix69g14r3y6flc64wq2p7x6t'
  ) THEN
    ALTER TABLE productos DROP CONSTRAINT "ukiix69g14r3y6flc64wq2p7x6t";
  END IF;
END $$;

-- Also drop by column in case the name differs (covers re-runs or schema drift)
DO $$ DECLARE r RECORD; BEGIN
  FOR r IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
    WHERE t.relname = 'productos' AND c.contype = 'u'
      AND a.attname = 'referencia'
      AND array_length(c.conkey, 1) = 1
  LOOP
    EXECUTE 'ALTER TABLE productos DROP CONSTRAINT "' || r.conname || '"';
  END LOOP;
END $$;

-- Add composite unique constraint: referencia must be unique per comercio, not globally
ALTER TABLE productos
  ADD CONSTRAINT uk_producto_referencia_comercio
  UNIQUE (referencia, comercio_id);
