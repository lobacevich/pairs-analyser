CREATE UNIQUE INDEX uk_price_records_symbol_fetched_at
    ON price_records (symbol, fetched_at);

CREATE INDEX idx_price_records_fetched_at ON price_records (fetched_at);