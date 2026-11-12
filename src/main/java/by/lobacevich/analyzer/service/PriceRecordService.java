package by.lobacevich.analyzer.service;

import by.lobacevich.analyzer.dto.PriceDtoResponse;

public interface PriceRecordService {
    void getAndSavePriceFromBinance();

    PriceDtoResponse getLatestRecord();
}
