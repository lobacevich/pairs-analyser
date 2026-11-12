package by.lobacevich.analyzer.service.impl;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.entity.PriceRecord;
import by.lobacevich.analyzer.repository.PriceRecordRepository;
import by.lobacevich.analyzer.service.PriceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class PriceRecordServiceImpl implements PriceRecordService {

    private final BinanceClient client;
    private final PriceRecordRepository repository;
    private @Value("${binance.symbol:BNBUSDT}") String symbol;

    public BinanceAvgPriceResponse getInformationFromBinance() {
        return client.fetchAveragePrice(symbol);
    }

    public void getAndSavePriceFromBinance() {
        BinanceAvgPriceResponse binanceResponse = client.fetchAveragePrice(symbol);
        repository.save(PriceRecord.builder()
                .symbol(symbol)
                .price(binanceResponse.price())
                .fetchedAt(Instant.now())
                .build());
    }
}
