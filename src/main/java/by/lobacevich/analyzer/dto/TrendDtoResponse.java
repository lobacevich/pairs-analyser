package by.lobacevich.analyzer.dto;

import by.lobacevich.analyzer.entity.enums.TrendType;

import java.time.LocalDate;

public record TrendDtoResponse(String symbol,
                               LocalDate start_date,
                               LocalDate end_date,
                               TrendType trend) {
}
