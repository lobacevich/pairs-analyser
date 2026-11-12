package by.lobacevich.analyzer.scheduler;

import by.lobacevich.analyzer.service.PriceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PriceScheduler {

    private final PriceRecordService service;

    @Scheduled(fixedDelayString = "${poll.interval}")
    public void poll() {
        service.getAndSavePriceFromBinance();
    }
}
