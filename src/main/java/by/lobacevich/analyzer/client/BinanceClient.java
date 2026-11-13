package by.lobacevich.analyzer.client;

import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.exception.BinanceApiException;
import by.lobacevich.analyzer.exception.BinanceRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RequiredArgsConstructor
@Component
public class BinanceClient {

    private static final int RATE_LIMIT = 429;
    private static final int AUTO_BANNED = 418;
    private static final int DEFAULT_RETRY_AFTER = 60;

    private final RestClient restClient;

    public BinanceAvgPriceResponse fetchAveragePrice(String symbol) {
        try {
            BinanceAvgPriceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/avgPrice")
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .body(BinanceAvgPriceResponse.class);

            if (response == null || response.price() == null) {
                throw new BinanceApiException("Empty response from Binance for " + symbol);
            }
            return response;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();

            if (status == RATE_LIMIT || status == AUTO_BANNED) {
                int retryAfter = extractRetryAfter(e);
                log.warn("Binance rate limit hit: status={}, body={}", status, body);
                throw new BinanceRetryableException("Binance rate limit, status " +
                        status + "retry after " + retryAfter,
                        retryAfter);
            }

            if (status >= 500) {
                log.warn("Binance server error: status={}", status);
                throw new BinanceRetryableException(
                        "Binance server error, status " + status,
                        DEFAULT_RETRY_AFTER);
            }

            log.error("Binance API error: status={}, body={}", status, body);
            throw new BinanceApiException("Binance API error, status " + status, e);

        } catch (BinanceApiException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error calling Binance", e);
            throw new BinanceRetryableException("Unexpected Binance error", DEFAULT_RETRY_AFTER);
        }
    }

    private int extractRetryAfter(RestClientResponseException e) {
        HttpHeaders headers = e.getResponseHeaders();
        if (headers != null) {
            String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
            if (value != null && !value.isBlank()) {
                try {
                    return Integer.parseInt(value.trim());
                } catch (NumberFormatException exception) {
                    log.warn("Cannot parse Retry-After header: '{}'", value);
                }
            }
        }
        return DEFAULT_RETRY_AFTER;
    }
}
