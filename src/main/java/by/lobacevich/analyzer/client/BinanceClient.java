package by.lobacevich.analyzer.client;

import by.lobacevich.analyzer.dto.AvgPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Log4j2
@RequiredArgsConstructor
@Service
public class BinanceClient {

    private final RestClient restClient;
    private @Value("${binance.symbol:BNBUSDT}") String symbol;

    public AvgPriceResponse fetchAveragePrice() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/avgPrice")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(AvgPriceResponse.class);
    }
}
