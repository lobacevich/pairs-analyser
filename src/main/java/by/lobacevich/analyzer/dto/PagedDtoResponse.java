package by.lobacevich.analyzer.dto;

import java.util.List;

public record PagedDtoResponse(List<PriceDtoResponse> items,
                               int page,
                               int size,
                               long totalElements,
                               boolean hasNext) {
}
