CREATE TABLE price_records (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(16) NOT NULL,
    price DECIMAL(10,2),
    fetched_at TIMESTAMP
);