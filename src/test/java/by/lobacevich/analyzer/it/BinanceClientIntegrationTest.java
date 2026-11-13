package by.lobacevich.analyzer.it;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.exception.BinanceApiException;
import by.lobacevich.analyzer.exception.BinanceRetryableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BinanceClientIntegrationTest {

    private static final String SYMBOL = "BNBUSDT";
    public static final BigDecimal PRICE = BigDecimal.valueOf(672.82896388);

    private static WireMockServer wireMock;

    private BinanceClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .requestFactory(factory)
                .build();

        client = new BinanceClient(restClient);
    }

    @AfterEach
    void reset() {
        wireMock.resetAll();
    }

    @Test
    void returnsParsedPriceOn200() {
        stub200("{\"mins\":5,\"price\":\"672.82896388\",\"closeTime\":1794480163093}");

        BinanceAvgPriceResponse actual = client.fetchAveragePrice(SYMBOL);

        assertThat(PRICE).isEqualByComparingTo(actual.price());
    }

    @Test
    void passesSymbolInQueryString() {
        stub200("{\"mins\":5,\"price\":\"100\"}");

        client.fetchAveragePrice("ETHUSDT");

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/api/v3/avgPrice"))
                .withQueryParam("symbol", equalTo("ETHUSDT")));
    }

    @Test
    void throwsOn200WithNullBody() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOf(BinanceApiException.class);
    }

    @Test
    void throwsOn200WithoutPrice() {
        stub200("{\"mins\":5}");

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOf(BinanceApiException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    void rateLimit429WithRetryAfter() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "120")));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRetryableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(120));
    }

    @Test
    void rateLimit429WithoutRetryAfterFallsBackTo60() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRetryableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(60));
    }

    @Test
    void rateLimit429WithInvalidRetryAfterFallsBackTo60() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "not-a-number")));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRetryableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(60));
    }

    @Test
    void autoBan418WithRetryAfter() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse()
                        .withStatus(418)
                        .withHeader("Retry-After", "600")));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRetryableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(600));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void serverErrorsAreRetryable(int status) {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRetryableException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(60));
    }

    @Test
    void clientError400() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.fetchAveragePrice("INVALID"))
                .isInstanceOf(BinanceApiException.class);
    }

    @Test
    void timeoutIsRetryable() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3000)));

        assertThatThrownBy(() -> client.fetchAveragePrice("BNBUSDT"))
                .isInstanceOf(BinanceRetryableException.class);
    }

    private void stub200(String body) {
        wireMock.stubFor(get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }
}
