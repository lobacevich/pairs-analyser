package by.lobacevich.analyzer.scheduler;

import by.lobacevich.analyzer.exception.BinanceApiException;
import by.lobacevich.analyzer.exception.BinanceRetryableException;
import by.lobacevich.analyzer.service.PriceRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
@Component
public class PriceScheduler {

    private final PriceRecordService service;

    private final AtomicReference<Instant> blockedUntil = new AtomicReference<>(Instant.MIN);

    @Scheduled(fixedDelayString = "${poll.interval}")
    public void poll() {
        Instant until = blockedUntil.get();
        if (Instant.now().isBefore(until)) {
            log.debug("Skipping poll: blocked until {}", until);
            return;
        }
        try {
            service.getAndSavePriceFromBinance();
            log.debug("Price polled and saved");
        } catch (BinanceRetryableException e) {
            applyBlock(e.getRetryAfterSeconds());
            log.warn("Poll blocked for {}s, until {}",
                    e.getRetryAfterSeconds(), blockedUntil.get());
        } catch (BinanceApiException e) {
            log.warn("Poll skipped due to Binance error: {}", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate record ignored (same symbol and fetchedAt)");
        } catch (Exception e) {
            log.error("Unexpected error during price poll", e);
        }
    }

    private void applyBlock(int seconds) {
        Instant until = Instant.now().plusSeconds(seconds);
        blockedUntil.accumulateAndGet(until,
                (current, next) -> next.isAfter(current) ? next : current);
    }
}
