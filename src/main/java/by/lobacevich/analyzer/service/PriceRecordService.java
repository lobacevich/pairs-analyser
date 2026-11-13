package by.lobacevich.analyzer.service;

import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.PagedDtoResponse;
import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.dto.TrendDtoResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface PriceRecordService {

    void getAndSavePriceFromBinance();

    @Transactional(readOnly = true)
    PriceDtoResponse getLatestRecord();

    @Transactional(readOnly = true)
    AveragePriceDtoResponse getAveragePrice(LocalDate from, LocalDate to);

    @Transactional(readOnly = true)
    PagedDtoResponse getPaged(LocalDate from,
                              LocalDate to,
                              int page,
                              int size);

    @Transactional(readOnly = true)
    TrendDtoResponse getTrend(LocalDate from,
                              LocalDate to);
}
