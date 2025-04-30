package com.crypto.analysis.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicator {
    private String market;
    private List<Double> sma20;
    private List<Double> ema20;
    private List<Double> rsi14;
    private LatestValues latest;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestValues {
        private Double sma20;
        private Double ema20;
        private Double rsi14;
    }
}
