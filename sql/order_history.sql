
ALTER TABLE order_history
  ALTER COLUMN create_date TYPE TIMESTAMP;

ALTER TABLE order_history
  ALTER COLUMN create_date DROP NOT NULL;

ALTER TABLE order_history
  ALTER COLUMN create_date SET DEFAULT NOW();


