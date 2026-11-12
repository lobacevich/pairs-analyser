package by.lobacevich.analyzer.controller;

import by.lobacevich.analyzer.dto.AvgPriceResponse;
import by.lobacevich.analyzer.service.impl.PriceRecordServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/prices")
public class PriceRecordController {

    private final PriceRecordServiceImpl service;

    @GetMapping
    public AvgPriceResponse getResponse() {
        return service.getInformation();
    }
}
