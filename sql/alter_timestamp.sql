 
ALTER TABLE inventory
	ALTER COLUMN last_updated TYPE TIMESTAMPTZ;

ALTER TABLE inventory
  ALTER COLUMN last_updated SET DEFAULT (NOW() AT TIME ZONE 'UTC');
 
--  

ALTER TABLE order_detail
	ALTER COLUMN create_date TYPE TIMESTAMPTZ;

ALTER TABLE order_detail
  ALTER COLUMN create_date SET DEFAULT (NOW() AT TIME ZONE 'UTC');  

--  

ALTER TABLE order_history
	ALTER COLUMN create_date TYPE TIMESTAMPTZ;

ALTER TABLE order_history
  ALTER COLUMN create_date SET DEFAULT (NOW() AT TIME ZONE 'UTC');    

--  

ALTER TABLE order_items
	ALTER COLUMN create_date TYPE TIMESTAMPTZ;

ALTER TABLE order_items
  ALTER COLUMN create_date SET DEFAULT (NOW() AT TIME ZONE 'UTC');      

--  

ALTER TABLE sales
	ALTER COLUMN date_sold TYPE TIMESTAMPTZ;

ALTER TABLE sales
  ALTER COLUMN date_sold SET DEFAULT (NOW() AT TIME ZONE 'UTC');      

