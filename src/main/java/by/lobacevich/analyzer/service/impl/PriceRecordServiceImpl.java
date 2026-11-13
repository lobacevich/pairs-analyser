package by.lobacevich.analyzer.service.impl;

import by.lobacevich.analyzer.client.BinanceClient;
import by.lobacevich.analyzer.dto.AveragePriceDtoResponse;
import by.lobacevich.analyzer.dto.BinanceAvgPriceResponse;
import by.lobacevich.analyzer.dto.InstantRange;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final String binanceSymbol;

    @Override
    public void getAndSavePriceFromBinance() {
        BinanceAvgPriceResponse binanceResponse = client.fetchAveragePrice(binanceSymbol);
        repository.save(PriceRecord.builder()
                .symbol(binanceSymbol)
                .price(binanceResponse.price())
                .fetchedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PriceDtoResponse getLatestRecord() {
        return mapper.entityToDto(repository.findTopByOrderByFetchedAtDesc()
                .orElseThrow(() -> new RecordsNotFoundException("There are no records yet")));
    }

    @Override
    @Transactional(readOnly = true)
    public AveragePriceDtoResponse getAveragePrice(LocalDate from, LocalDate to) {
        InstantRange range = getInstantRange(from, to);
        return new AveragePriceDtoResponse(repository.findAveragePriceBetween(
                        binanceSymbol,
                        range.from(),
                        range.to())
                .orElse(BigDecimal.ZERO));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDtoResponse getPaged(LocalDate from,
                                     LocalDate to,
                                     int page,
                                     int size) {
        InstantRange range = getInstantRange(from, to);
        Page<PriceRecord> pricePage = repository.findBySymbolAndFetchedAtBetween(
                binanceSymbol,
                range.from(),
                range.to(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fetchedAt")));

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
    @Transactional(readOnly = true)
    public TrendDtoResponse getTrend(LocalDate from,
                                     LocalDate to) {
        InstantRange range = getInstantRange(from, to);
        List<PriceRecord> records = repository.findByFetchedAtBetweenOrderByFetchedAtAsc(
                binanceSymbol,
                range.from(),
                range.to());
        return new TrendDtoResponse(binanceSymbol, from, to, trendAnalyzer.calculate(records));
    }

    private InstantRange getInstantRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        return new InstantRange(from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
