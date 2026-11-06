CREATE TABLE inventory_item (
    sku VARCHAR(128) PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE inventory_reservation (
    order_id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED'))
);

CREATE TABLE inventory_reservation_line (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES inventory_reservation(order_id) ON DELETE CASCADE,
    sku VARCHAR(128) NOT NULL REFERENCES inventory_item(sku),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (order_id, sku)
);
