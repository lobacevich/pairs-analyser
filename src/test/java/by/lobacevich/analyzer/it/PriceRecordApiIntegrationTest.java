package by.lobacevich.analyzer.it;

import by.lobacevich.analyzer.entity.PriceRecord;
import by.lobacevich.analyzer.repository.PriceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PriceRecordApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.6")
                    .withDatabaseName("test-db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String BASE_URL = "/api/v1/prices";
    private static final String FROM = "2026-11-01";
    private static final String TO = "2026-11-13";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceRecordRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void getLatestRecord_ShouldReturnPriceDtoResponse() throws Exception {
        insertRecord("100.00", "2026-11-05T10:00:00Z");
        insertRecord("200.50", "2026-11-06T10:00:00Z");
        insertRecord("150.25", "2026-11-04T10:00:00Z");

        mockMvc.perform(get(BASE_URL + "/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.price", closeTo(200.50, 0.01)))
                .andExpect(jsonPath("$.fetchedAt").exists());
    }

    @Test
    void getLatestRecord_ShouldReturnStatus404_WhenNoData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/latest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAverage_ShouldReturnAveragePrice() throws Exception {
        insertRecord("100", "2026-11-05T10:00:00Z");
        insertRecord("200", "2026-11-06T10:00:00Z");
        insertRecord("300", "2026-11-07T10:00:00Z");

        mockMvc.perform(get(BASE_URL + "/average")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePrice", closeTo(200.0, 0.01)));
    }

    @Test
    void getAverage_ShouldReturnZero_WhenNoData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/average")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePrice").value(0));
    }

    @Test
    void getAverage_ShouldReturn400_WhenFromAfterTo() throws Exception {
        mockMvc.perform(get(BASE_URL + "/average")
                        .param("from", TO)
                        .param("to", FROM))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAverage_ShouldReturn400_WhenDateFormatInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/average")
                        .param("from", "13-11-2026")
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPage_ShouldReturnItemsWithMetadata() throws Exception {
        for (int i = 0; i < 5; i++) {
            insertRecord(String.valueOf(100 + i),
                    "2026-11-0" + (5 + i) + "T10:00:00Z");
        }

        mockMvc.perform(get(BASE_URL)
                        .param("from", FROM)
                        .param("to", TO)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getPage_ShouldSortByFetchedAtDescending() throws Exception {
        insertRecord("100", "2026-11-04T10:00:00Z");   // старее
        insertRecord("200", "2026-11-06T10:00:00Z");   // самый свежий
        insertRecord("150", "2026-11-05T10:00:00Z");   // в середине

        mockMvc.perform(get(BASE_URL)
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price", closeTo(200.0, 0.01)))
                .andExpect(jsonPath("$.items[1].price", closeTo(150.0, 0.01)))
                .andExpect(jsonPath("$.items[2].price", closeTo(100.0, 0.01)));
    }

    @Test
    void getPage_ShouldReturnEmptyPage_WhenNoData() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getPage_ShouldReturn400_WhenSizeExceeds100() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("from", FROM)
                        .param("to", TO)
                        .param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPage_ShouldReturn400_WhenPageIsNegative() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("from", FROM)
                        .param("to", TO)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPage_ShouldReturn400_WhenFromAfterTo() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("from", TO)
                        .param("to", FROM))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTrend_ShouldReturnUpward_WhenPricesGrow() throws Exception {
        insertRecord("100", "2026-11-01T10:00:00Z");
        insertRecord("110", "2026-11-02T10:00:00Z");
        insertRecord("120", "2026-11-03T10:00:00Z");
        insertRecord("130", "2026-11-04T10:00:00Z");
        insertRecord("140", "2026-11-05T10:00:00Z");

        mockMvc.perform(get(BASE_URL + "/trend")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("UPWARD"))
                .andExpect(jsonPath("$.start_date").value(FROM))
                .andExpect(jsonPath("$.end_date").value(TO))
                .andExpect(jsonPath("$.symbol").exists());
    }

    @Test
    void getTrend_ShouldReturnDownward_WhenPricesFall() throws Exception {
        insertRecord("140", "2026-11-01T10:00:00Z");
        insertRecord("130", "2026-11-02T10:00:00Z");
        insertRecord("120", "2026-11-03T10:00:00Z");
        insertRecord("110", "2026-11-04T10:00:00Z");
        insertRecord("100", "2026-11-05T10:00:00Z");

        mockMvc.perform(get(BASE_URL + "/trend")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("DOWNWARD"));
    }

    @Test
    void getTrend_ShouldReturnIndefinite_WhenNotEnoughData() throws Exception {
        insertRecord("100", "2026-11-05T10:00:00Z");
        mockMvc.perform(get(BASE_URL + "/trend")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("INDEFINITE"));
    }

    @Test
    void getTrend_ShouldReturnFlat_WhenPricesZigzagWithoutTrend() throws Exception {
        insertRecord("100", "2026-11-01T10:00:00Z");
        insertRecord("90",  "2026-11-02T10:00:00Z");
        insertRecord("100", "2026-11-03T10:00:00Z");
        insertRecord("90",  "2026-11-04T10:00:00Z");
        insertRecord("100", "2026-11-05T10:00:00Z");

        mockMvc.perform(get(BASE_URL + "/trend")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("FLAT"));
    }

    @Test
    void getTrend_ShouldReturn400_WhenFromAfterTo() throws Exception {
        mockMvc.perform(get(BASE_URL + "/trend")
                        .param("from", TO)
                        .param("to", FROM))
                .andExpect(status().isBadRequest());
    }

    private void insertRecord(String price, String fetchedAt) {
        repository.save(PriceRecord.builder()
                .symbol("BNBUSDT")
                .price(new BigDecimal(price))
                .fetchedAt(Instant.parse(fetchedAt))
                .build());
    }
}
