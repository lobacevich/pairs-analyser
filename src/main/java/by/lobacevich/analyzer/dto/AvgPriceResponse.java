package by.lobacevich.analyzer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AvgPriceResponse(
        String mins,
        BigDecimal price,
        Instant closeTime) {
}
