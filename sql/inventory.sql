
-- DROP TABLE IF EXISTS inventory;

-- CREATE TABLE inventory (
-- 	id BIGSERIAL NOT NULL PRIMARY KEY,
-- 	last_updated TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
-- 	sku VARCHAR(50) UNIQUE NOT NULL,
--     product_name VARCHAR(50) NOT NULL,
--     on_hand INTEGER DEFAULT 0,
--     trigger INTEGER DEFAULT 17,
--     selling BOOLEAN DEFAULT true);

ALTER TABLE inventory
	ALTER COLUMN last_updated TYPE TIMESTAMPTZ;

ALTER TABLE inventory
  ALTER COLUMN last_updated SET DEFAULT (NOW() AT TIME ZONE 'UTC');