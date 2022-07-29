DROP TABLE IF EXISTS inventory;

CREATE TABLE inventory (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	last_updated DATE DEFAULT NOW(),
	sku VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    on_hand INTEGER DEFAULT 0);


ALTER TABLE inventory
ADD COLUMN trigger INTEGER DEFAULT 17,
ADD COLUMN selling BOOLEAN DEFAULT true;
