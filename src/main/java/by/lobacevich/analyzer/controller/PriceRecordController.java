package by.lobacevich.analyzer.controller;

import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.PagedDtoResponse;
import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.dto.TrendDtoResponse;
import by.lobacevich.analyzer.service.PriceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Prices", description = "Endpoints for retrieving stored prices and analytics")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/prices")
public class PriceRecordController {

    private final PriceRecordService service;

    @GetMapping("/latest")
    @Operation(
            summary = "Get the latest saved record",
            description = "Returns the most recent price fetched from Binance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest record returned successfully"),
            @ApiResponse(responseCode = "404", description = "No price records have been saved yet")
    })
    public PriceDtoResponse getResponse() {
        return service.getLatestRecord();
    }

    @GetMapping("/average")
    @Operation(
            summary = "Get average price for a period",
            description = "Calculates the arithmetic mean of prices within the given interval. Dates: YYYY-MM-DD."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Average price calculated"),
            @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    public AveragePriceDtoResponse average(
            @Parameter(description = "Start date", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date", example = "2026-11-13")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getAveragePrice(from, to);
    }

    @GetMapping
    @Operation(
            summary = "Get paginated records for a period",
            description = "Returns a page of stored price records. Dates: YYYY-MM-DD."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of records returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public PagedDtoResponse getPage(
            @Parameter(description = "Start date", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date", example = "2026-11-13")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return service.getPaged(from, to, page, size);
    }

    @GetMapping("/trend")
    @Operation(
            summary = "Determine price trend for a period",
            description = """
                    Classifies the price movement (UPWARD, DOWNWARD, FLAT) using linear regression
                    over all records in the interval."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trend calculated"),
            @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    public TrendDtoResponse trend(
            @Parameter(description = "Start date", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date", example = "2026-11-13")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getTrend(from, to);
    }
}
