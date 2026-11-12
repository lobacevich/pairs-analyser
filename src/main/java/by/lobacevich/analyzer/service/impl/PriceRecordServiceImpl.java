package by.lobacevich.analyzer.service.impl;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.AvgPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PriceRecordServiceImpl {

    private final BinanceClient client;

    public AvgPriceResponse getInformation() {
        return client.fetchAveragePrice();
    }
}
