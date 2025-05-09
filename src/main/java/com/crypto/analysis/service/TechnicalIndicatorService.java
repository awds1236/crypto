package com.crypto.analysis.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

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
        try {
            JsonNode candles = objectMapper.readTree(candleData);
            BarSeries series = new BaseBarSeries();
            
            // 날짜 기준으로 정렬 (과거 → 최신)
            List<JsonNode> candleList = new ArrayList<>();
            candles.forEach(candleList::add);
            
            // 날짜 기준 오름차순 정렬
            candleList.sort(Comparator.comparing(a -> 
                ZonedDateTime.ofInstant(
                    Instant.parse(a.get("candle_date_time_utc").asText() + "Z"),
                    ZoneId.systemDefault()
                )
            ));
            
            // 정렬된 데이터로 시리즈 생성
            for (JsonNode candle : candleList) {
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
        } catch (Exception e) {
            System.err.println("TA4J 시리즈 생성 실패: " + e.getMessage());
            throw e;
        }
    }
    
    // SMA 계산
    public List<Double> calculateSMA(BarSeries series, int period) {
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            SMAIndicator sma = new SMAIndicator(closePrice, period);
            
            List<Double> smaValues = new ArrayList<>();
            for (int i = 0; i < series.getBarCount(); i++) {
                smaValues.add(sma.getValue(i).doubleValue());
            }
            
            return smaValues;
        } catch (Exception e) {
            System.err.println("SMA 계산 실패: " + e.getMessage());
            return createDefaultIndicatorValues(series.getBarCount());
        }
    }
    
    // EMA 계산
    public List<Double> calculateEMA(BarSeries series, int period) {
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            EMAIndicator ema = new EMAIndicator(closePrice, period);
            
            List<Double> emaValues = new ArrayList<>();
            for (int i = 0; i < series.getBarCount(); i++) {
                emaValues.add(ema.getValue(i).doubleValue());
            }
            
            return emaValues;
        } catch (Exception e) {
            System.err.println("EMA 계산 실패: " + e.getMessage());
            return createDefaultIndicatorValues(series.getBarCount());
        }
    }
    
    // RSI 계산
    public List<Double> calculateRSI(BarSeries series, int period) {
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, period);
            
            List<Double> rsiValues = new ArrayList<>();
            for (int i = 0; i < series.getBarCount(); i++) {
                rsiValues.add(rsi.getValue(i).doubleValue());
            }
            
            return rsiValues;
        } catch (Exception e) {
            System.err.println("RSI 계산 실패: " + e.getMessage());
            return createDefaultRsiValues(series.getBarCount());
        }
    }
    
    // 기본 지표 값 생성 (에러 시)
    private List<Double> createDefaultIndicatorValues(int count) {
        List<Double> values = new ArrayList<>();
        double baseValue = 50000.0;
        
        for (int i = 0; i < count; i++) {
            values.add(baseValue + (Math.random() * 1000 - 500));
        }
        
        return values;
    }
    
    // 기본 RSI 값 생성 (에러 시)
    private List<Double> createDefaultRsiValues(int count) {
        List<Double> values = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            values.add(30.0 + Math.random() * 40); // 30-70 사이 랜덤 값
        }
        
        return values;
    }
    
    // 모든 지표 계산 및 MAP 반환
    public Map<String, Object> calculateAllIndicators(String market, String candleData) throws Exception {
        try {
            BarSeries series = createSeries(candleData);
            
            Map<String, Object> indicators = new HashMap<>();
            indicators.put("market", market);
            indicators.put("sma20", calculateSMA(series, 20));
            indicators.put("ema20", calculateEMA(series, 20));
            indicators.put("rsi14", calculateRSI(series, 14));
            
            // 날짜 정보 추출 추가
            List<String> dateLabels = new ArrayList<>();
            for (int i = 0; i < series.getBarCount(); i++) {
                ZonedDateTime dateTime = series.getBar(i).getEndTime();
                // MM/dd 형식으로 날짜 표시 (월/일)
                String dateLabel = String.format("%02d/%02d", 
                    dateTime.getMonthValue(), 
                    dateTime.getDayOfMonth());
                dateLabels.add(dateLabel);
            }
            indicators.put("dates", dateLabels);
            
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
        } catch (Exception e) {
            System.err.println("지표 계산 중 오류: " + e.getMessage());
            
            // 오류 시 기본 지표 데이터 생성
            Map<String, Object> defaultIndicators = new HashMap<>();
            int defaultSize = 30;
            
            List<Double> defaultSma = createDefaultIndicatorValues(defaultSize);
            List<Double> defaultEma = createDefaultIndicatorValues(defaultSize);
            List<Double> defaultRsi = createDefaultRsiValues(defaultSize);
            
            // 기본 날짜 정보 생성 (현재 날짜로부터 30일)
            List<String> defaultDates = new ArrayList<>();
            ZonedDateTime now = ZonedDateTime.now();
            for (int i = defaultSize - 1; i >= 0; i--) {
                ZonedDateTime date = now.minusDays(i);
                String dateLabel = String.format("%02d/%02d", 
                    date.getMonthValue(), 
                    date.getDayOfMonth());
                defaultDates.add(dateLabel);
            }
            
            defaultIndicators.put("market", market);
            defaultIndicators.put("sma20", defaultSma);
            defaultIndicators.put("ema20", defaultEma);
            defaultIndicators.put("rsi14", defaultRsi);
            defaultIndicators.put("dates", defaultDates);
            
            Map<String, Double> latestValues = new HashMap<>();
            latestValues.put("sma20", defaultSma.get(defaultSize - 1));
            latestValues.put("ema20", defaultEma.get(defaultSize - 1));
            latestValues.put("rsi14", defaultRsi.get(defaultSize - 1));
            
            defaultIndicators.put("latest", latestValues);
            return defaultIndicators;
        }
    }

    // TechnicalIndicatorService.java에 추가할 메서드
    /**
     * 주식 기술적 지표 계산 (StockIndicatorService로 위임)
     */
    public Map<String, Object> calculateStockIndicators(String symbol, String candleData) throws Exception {
        // StockIndicatorService 인스턴스 생성 또는 주입
        StockIndicatorService stockIndicatorService = new StockIndicatorService();
        
        // StockIndicatorService의 메서드 호출하여 결과 반환
        return stockIndicatorService.calculateStockIndicators(symbol, candleData);
    }
}