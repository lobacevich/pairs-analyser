package by.lobacevich.analyzer.service.impl;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.dto.PagedDtoResponse;
import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.dto.TrendDtoResponse;
import by.lobacevich.analyzer.entity.PriceRecord;
import by.lobacevich.analyzer.exception.RecordsNotFoundException;
import by.lobacevich.analyzer.mapper.PriceRecordMapper;
import by.lobacevich.analyzer.repository.PriceRecordRepository;
import by.lobacevich.analyzer.service.PriceRecordService;
import by.lobacevich.analyzer.service.TrendAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PriceRecordServiceImpl implements PriceRecordService {

    private final BinanceClient client;
    private final PriceRecordRepository repository;
    private final PriceRecordMapper mapper;
    private final TrendAnalyzer trendAnalyzer;
    private @Value("${binance.symbol:BNBUSDT}") String symbol;

    @Override
    public void getAndSavePriceFromBinance() {
        BinanceAvgPriceResponse binanceResponse = client.fetchAveragePrice(symbol);
        repository.save(PriceRecord.builder()
                .symbol(symbol)
                .price(binanceResponse.price())
                .fetchedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                .build());
    }

    @Override
    public PriceDtoResponse getLatestRecord() {
        return mapper.entityToDto(repository.findTopByOrderByFetchedAtDesc()
                .orElseThrow(() -> new RecordsNotFoundException("There are no records yet")));
    }

    @Override
    public AveragePriceDtoResponse getAveragePrice(LocalDate from, LocalDate to) {
        return new AveragePriceDtoResponse(repository.findAveragePriceBetween(localDateToInstant(from),
                        localDateToInstant(to.plusDays(1)))
                .orElse(BigDecimal.ZERO));
    }

    @Override
    public PagedDtoResponse getPaged(LocalDate from,
                                     LocalDate to,
                                     int page,
                                     int size) {
        Page<PriceRecord> pricePage = repository.findByFetchedAtBetween(localDateToInstant(from),
                localDateToInstant(to.plusDays(1)),
                PageRequest.of(page, size));

        List<PriceDtoResponse> items = pricePage.stream()
                .map(mapper::entityToDto)
                .toList();
        return new PagedDtoResponse(items,
                pricePage.getNumber(),
                pricePage.getSize(),
                pricePage.getTotalElements(),
                pricePage.hasNext());
    }

    @Override
    public TrendDtoResponse getTrend(LocalDate from,
                                     LocalDate to) {
        List<PriceRecord> records = repository.findByFetchedAtBetweenOrderByFetchedAtAsc(localDateToInstant(from),
                localDateToInstant(to.plusDays(1)));
        return new TrendDtoResponse(symbol, from, to, trendAnalyzer.calculate(records));
    }

    private Instant localDateToInstant(LocalDate from) {
        return from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
