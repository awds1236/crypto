package com.crypto.analysis.service;

import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TechnicalIndicatorService {
    
    private final ObjectMapper objectMapper;
    
    public TechnicalIndicatorService() {
        this.objectMapper = new ObjectMapper();
    }
    
    // 업비트 캔들 데이터를 TA4J 시리즈로 변환
    public BarSeries createSeries(String candleData) throws Exception {
        JsonNode candles = objectMapper.readTree(candleData);
        BarSeries series = new BaseBarSeries();
        
        for (JsonNode candle : candles) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                Instant.parse(candle.get("candle_date_time_utc").asText() + "Z"),
                ZoneId.systemDefault()
            );
            
            double openPrice = candle.get("opening_price").asDouble();
            double highPrice = candle.get("high_price").asDouble();
            double lowPrice = candle.get("low_price").asDouble();
            double closePrice = candle.get("trade_price").asDouble();
            double volume = candle.get("candle_acc_trade_volume").asDouble();
            
            series.addBar(dateTime, openPrice, highPrice, lowPrice, closePrice, volume);
        }
        
        return series;
    }
    
    // SMA 계산
    public List<Double> calculateSMA(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        
        List<Double> smaValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            smaValues.add(sma.getValue(i).doubleValue());
        }
        
        return smaValues;
    }
    
    // EMA 계산
    public List<Double> calculateEMA(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, period);
        
        List<Double> emaValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            emaValues.add(ema.getValue(i).doubleValue());
        }
        
        return emaValues;
    }
    
    // RSI 계산
    public List<Double> calculateRSI(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        
        List<Double> rsiValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            rsiValues.add(rsi.getValue(i).doubleValue());
        }
        
        return rsiValues;
    }
    
    // 모든 지표 계산 및 MAP 반환
    public Map<String, Object> calculateAllIndicators(String market, String candleData) throws Exception {
        BarSeries series = createSeries(candleData);
        
        Map<String, Object> indicators = new HashMap<>();
        indicators.put("market", market);
        indicators.put("sma20", calculateSMA(series, 20));
        indicators.put("ema20", calculateEMA(series, 20));
        indicators.put("rsi14", calculateRSI(series, 14));
        
        // 최신 지표 값만 추출
        Map<String, Double> latestValues = new HashMap<>();
        List<Double> sma20List = (List<Double>) indicators.get("sma20");
        List<Double> ema20List = (List<Double>) indicators.get("ema20");
        List<Double> rsi14List = (List<Double>) indicators.get("rsi14");
        
        latestValues.put("sma20", sma20List.get(series.getBarCount() - 1));
        latestValues.put("ema20", ema20List.get(series.getBarCount() - 1));
        latestValues.put("rsi14", rsi14List.get(series.getBarCount() - 1));
        
        indicators.put("latest", latestValues);
        return indicators;
    }
}
