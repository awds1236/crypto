package com.crypto.analysis.model;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private String market;
    private ZonedDateTime candleDateTimeUtc;
    private ZonedDateTime candleDateTimeKst;
    private Double openingPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double tradePrice;
    private Double candleAccTradePrice;
    private Double candleAccTradeVolume;
}
