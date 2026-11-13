package by.lobacevich.analyzer.service;

import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.PagedDtoResponse;
import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.dto.TrendDtoResponse;

import java.time.LocalDate;

public interface PriceRecordService {
    void getAndSavePriceFromBinance();

    PriceDtoResponse getLatestRecord();

    AveragePriceDtoResponse getAveragePrice(LocalDate from, LocalDate to);

    PagedDtoResponse getPaged(LocalDate from,
                              LocalDate to,
                              int number,
                              int size);

    TrendDtoResponse getTrend(LocalDate from,
                              LocalDate to);
}
