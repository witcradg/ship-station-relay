
DROP TABLE IF EXISTS order_history;

CREATE TABLE order_history (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	create_date TIMESTAMP DEFAULT NOW(),
	order_number VARCHAR(50) NOT NULL,
    order_status VARCHAR(50) NOT NULL);



DROP TABLE IF EXISTS order_items;

CREATE TABLE order_items (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	create_date TIMESTAMP DEFAULT NOW(),
	order_number VARCHAR(50) NOT NULL,
    order_items TEXT NOT NULL);



DROP TABLE IF EXISTS inventory;

CREATE TABLE inventory (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	last_updated DATE DEFAULT NOW(),
	sku VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    on_hand INTEGER DEFAULT 0);



DROP TABLE IF EXISTS sales;

CREATE TABLE sales (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	date_sold TIMESTAMP DEFAULT NOW(),
    order_number VARCHAR(50) NOT NULL,
	sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    unit_price MONEY NOT NULL,
    total_price MONEY NOT NULL, 
    quantity_sold INTEGER);



DROP TABLE IF EXISTS order_detail;

CREATE TABLE order_detail (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	create_date TIMESTAMP DEFAULT NOW(),
    order_number VARCHAR(50) NOT NULL,
    items_total  MONEY NOT NULL,
    saved_amount MONEY NOT NULL,
    subtotal MONEY NOT NULL,
    shipping_fees MONEY NOT NULL,
    taxes_total MONEY NOT NULL,
    grand_total MONEY NOT NULL,
    base_total MONEY NOT NULL);

