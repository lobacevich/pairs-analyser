package by.lobacevich.analyzer.service.impl;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.dto.PagedDtoResponse;
import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.dto.TrendDtoResponse;
import by.lobacevich.analyzer.entity.PriceRecord;
import by.lobacevich.analyzer.entity.enums.TrendType;
import by.lobacevich.analyzer.exception.RecordsNotFoundException;
import by.lobacevich.analyzer.mapper.PriceRecordMapper;
import by.lobacevich.analyzer.repository.PriceRecordRepository;
import by.lobacevich.analyzer.service.TrendAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceRecordServiceImplTest {

    private static final String SYMBOL = "BNBUSDT";
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 13);
    private static final BigDecimal PRICE = BigDecimal.valueOf(500);
    private static final Instant INSTANT_FROM = FROM.atStartOfDay(ZoneOffset.UTC).toInstant();
    private static final Instant INSTANT_TO = TO.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    @Mock
    private BinanceClient client;

    @Mock
    private PriceRecordRepository repository;

    @Mock
    private PriceRecordMapper mapper;

    @Mock
    private TrendAnalyzer trendAnalyzer;

    @Mock
    private BinanceAvgPriceResponse binanceResponse;

    @Mock
    private PriceRecord record;

    @Mock
    private PriceDtoResponse recordDto;

    private PriceRecordServiceImpl service;

    @Captor
    private ArgumentCaptor<PriceRecord> recordArgumentCaptor;

    @BeforeEach
    void setUp() {
        service = new PriceRecordServiceImpl(client, repository, mapper, trendAnalyzer, SYMBOL);
    }

    @Test
    void getAndSavePriceFromBinance_ShouldSaveRecordWithCorrectFields() {
        when(client.fetchAveragePrice(SYMBOL)).thenReturn(binanceResponse);
        when(binanceResponse.price()).thenReturn(PRICE);

        service.getAndSavePriceFromBinance();

        verify(repository, times(1)).save(recordArgumentCaptor.capture());
        assertEquals(PRICE, recordArgumentCaptor.getValue().getPrice());
        assertEquals(SYMBOL, recordArgumentCaptor.getValue().getSymbol());
    }

    @Test
    void getLatestRecord_ShouldReturnPriceDtoResponse() {
        when(repository.findTopByOrderByFetchedAtDesc()).thenReturn(Optional.of(record));
        when(mapper.entityToDto(record)).thenReturn(recordDto);

        PriceDtoResponse actual = service.getLatestRecord();

        assertEquals(recordDto, actual);
    }

    @Test
    void getLatestRecord_ShouldThrowRecordsNotFoundException() {
        when(repository.findTopByOrderByFetchedAtDesc()).thenReturn(Optional.empty());

        assertThrows(RecordsNotFoundException.class, () -> service.getLatestRecord());
    }

    @Test
    void getAveragePrice_ShouldCallMethodOfRepositoryAndReturnAveragePriceDtoResponse() {
        when(repository.findAveragePriceBetween(INSTANT_FROM, INSTANT_TO)).thenReturn(Optional.of(PRICE));

        AveragePriceDtoResponse actual = service.getAveragePrice(FROM, TO);

        assertEquals(PRICE, actual.averagePrice());
    }

    @Test
    void getAveragePrice_ShouldReturnAveragePriceZero() {
        when(repository.findAveragePriceBetween(INSTANT_FROM, INSTANT_TO)).thenReturn(Optional.empty());

        AveragePriceDtoResponse actual = service.getAveragePrice(FROM, TO);

        assertEquals(BigDecimal.ZERO, actual.averagePrice());
    }

    @Test
    void getAveragePrice_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getAveragePrice(TO, FROM));
    }

    @Test
    void getPaged_ShouldReturnPagedDtoResponse() {
        Page<PriceRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 20), 135);

        when(repository.findByFetchedAtBetween(any(), any(), any())).thenReturn(page);
        when(mapper.entityToDto(record)).thenReturn(recordDto);

        PagedDtoResponse actual = service.getPaged(FROM, TO, 0, 20);

        assertTrue(actual.items().contains(recordDto));
        assertEquals(0, actual.page());
        assertEquals(20, actual.size());
        assertEquals(135, actual.totalElements());
        assertTrue(actual.hasNext());
    }

    @Test
    void getTrend_ShouldReturnTrendDtoResponse() {
        when(repository.findByFetchedAtBetweenOrderByFetchedAtAsc(INSTANT_FROM, INSTANT_TO)).thenReturn(List.of(record));
        when(trendAnalyzer.calculate(List.of(record))).thenReturn(TrendType.DOWNWARD);

        TrendDtoResponse actual = service.getTrend(FROM, TO);

        assertEquals(SYMBOL, actual.symbol());
        assertEquals(FROM, actual.start_date());
        assertEquals(TO, actual.end_date());
        assertEquals(TrendType.DOWNWARD, actual.trend());
    }
}