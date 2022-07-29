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

    