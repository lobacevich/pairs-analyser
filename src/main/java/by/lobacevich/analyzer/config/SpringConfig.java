package by.lobacevich.analyzer.config;

import by.lobacevich.analyzer.mapper.PriceRecordMapper;
import by.lobacevich.analyzer.mapper.PriceRecordMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SpringConfig {

    @Bean
    PriceRecordMapper priceRecordMapper() {
        return new PriceRecordMapperImpl();
    }
}
