package by.lobacevich.analyzer.service;

import by.lobacevich.analyzer.entity.PriceRecord;
import by.lobacevich.analyzer.entity.enums.TrendType;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrendAnalyzer {

    private final double minSlopeThreshold;
    private final double minRSquare;

    public TrendAnalyzer(
            @Value("${trend.min-slope-threshold:0.001}") double minSlopeThreshold,
            @Value("${trend.min-r-square:0.3}") double minRSquare) {
        this.minSlopeThreshold = minSlopeThreshold;
        this.minRSquare = minRSquare;
    }

    public TrendType calculate(List<PriceRecord> records) {
        if (records == null || records.size() < 2) {
            return TrendType.FLAT;
    }

        SimpleRegression regression = new SimpleRegression();
        long startMillis = records.getFirst().getFetchedAt().toEpochMilli();

        for (PriceRecord record : records) {
            double x = (record.getFetchedAt().toEpochMilli() - startMillis) / (1000.0 * 60 * 60 * 24);
            double y = record.getPrice().doubleValue();
            regression.addData(x, y);
        }

        double slope = regression.getSlope();
        double rSquare = regression.getRSquare();
        double meanPrice = records.stream().mapToDouble(r -> r.getPrice().doubleValue()).average().orElse(1.0);

        double normalizedSlope = slope / meanPrice;

        if (rSquare >= minRSquare) {
            if (normalizedSlope > minSlopeThreshold) {
                return TrendType.UPWARD;
            } else if (normalizedSlope < -minSlopeThreshold) {
                return TrendType.DOWNWARD;
            }
        }

        return TrendType.FLAT;
    }
}
