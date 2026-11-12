package by.lobacevich.analyzer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceDtoResponse(
        String symbol,
        BigDecimal price,
        Instant fetchedAt) {
}
