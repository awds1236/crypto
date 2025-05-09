package com.crypto.analysis.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 주식 기술적 지표 계산 서비스
 */
@Service
public class StockIndicatorService {
    
    private final ObjectMapper objectMapper;
    
    public StockIndicatorService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 주식 기술적 지표 계산
     */
    public Map<String, Object> calculateStockIndicators(String symbol, String candleData) throws Exception {
        try {
            BarSeries series = createSeriesFromStockData(candleData);
            
            Map<String, Object> indicators = new HashMap<>();
            indicators.put("symbol", symbol);
            
            // 시리즈에서 가격/날짜/거래량 추출
            List<Double> prices = extractPrices(series);
            List<Double> volumes = extractVolumes(series);
            List<String> dates = extractDates(series);
            
            indicators.put("prices", prices);
            indicators.put("volumes", volumes);
            indicators.put("dates", dates);
            
            // SMA 계산
            indicators.put("sma20", calculateSMA(series, 20));
            indicators.put("sma50", calculateSMA(series, 50));
            indicators.put("sma200", calculateSMA(series, 200));
            
            // EMA 계산
            indicators.put("ema20", calculateEMA(series, 20));
            
            // RSI 계산
            indicators.put("rsi", calculateRSI(series, 14));
            
            // MACD 계산
            Map<String, List<Double>> macdData = calculateMACD(series);
            indicators.put("macd", macdData.get("macd"));
            indicators.put("macdSignal", macdData.get("signal"));
            indicators.put("macdHist", macdData.get("histogram"));
            
            // 볼린저 밴드 계산
            Map<String, List<Double>> bbData = calculateBollingerBands(series);
            indicators.put("bbUpper", bbData.get("upper"));
            indicators.put("bbMiddle", bbData.get("middle"));
            indicators.put("bbLower", bbData.get("lower"));
            
            // 최신 지표 값만 추출
            Map<String, Double> latestValues = extractLatestValues(indicators);
            indicators.put("latest", latestValues);
            
            return indicators;
        } catch (Exception e) {
            System.err.println("주식 지표 계산 중 오류: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 주식 캔들 데이터를 TA4J 시리즈로 변환
     */
    private BarSeries createSeriesFromStockData(String candleData) throws Exception {
        try {
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
        } catch (Exception e) {
            System.err.println("TA4J 시리즈 생성 실패: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 시리즈에서 종가 추출
     */
    private List<Double> extractPrices(BarSeries series) {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            prices.add(series.getBar(i).getClosePrice().doubleValue());
        }
        return prices;
    }
    
    /**
     * 시리즈에서 거래량 추출
     */
    private List<Double> extractVolumes(BarSeries series) {
        List<Double> volumes = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            volumes.add(series.getBar(i).getVolume().doubleValue());
        }
        return volumes;
    }
    
    /**
     * 시리즈에서 날짜 추출
     */
    private List<String> extractDates(BarSeries series) {
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime dateTime = series.getBar(i).getEndTime();
            // MM/dd 형식으로 날짜 표시 (월/일)
            String dateLabel = String.format("%02d/%02d", 
                dateTime.getMonthValue(), 
                dateTime.getDayOfMonth());
            dates.add(dateLabel);
        }
        return dates;
    }
    
    /**
     * SMA 계산
     */
    private List<Double> calculateSMA(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        
        List<Double> smaValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            // 데이터가 충분하지 않은 초기 구간은 종가로 대체
            double smaValue = i < period - 1 ? 
                closePrice.getValue(i).doubleValue() :
                sma.getValue(i).doubleValue();
            smaValues.add(smaValue);
        }
        
        return smaValues;
    }
    
    /**
     * EMA 계산
     */
    private List<Double> calculateEMA(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, period);
        
        List<Double> emaValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            // 데이터가 충분하지 않은 초기 구간은 종가로 대체
            double emaValue = i < period - 1 ? 
                closePrice.getValue(i).doubleValue() :
                ema.getValue(i).doubleValue();
            emaValues.add(emaValue);
        }
        
        return emaValues;
    }
    
    /**
     * RSI 계산
     */
    private List<Double> calculateRSI(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        
        List<Double> rsiValues = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            // 충분한 데이터가 없는 경우 50으로 기본값 설정
            double rsiValue = i < period ? 50.0 : rsi.getValue(i).doubleValue();
            rsiValues.add(rsiValue);
        }
        
        return rsiValues;
    }
    
    /**
     * MACD 계산
     */
    private Map<String, List<Double>> calculateMACD(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator signal = new EMAIndicator(macd, 9);
        
        List<Double> macdValues = new ArrayList<>();
        List<Double> signalValues = new ArrayList<>();
        List<Double> histogramValues = new ArrayList<>();
        
        for (int i = 0; i < series.getBarCount(); i++) {
            double macdValue = macd.getValue(i).doubleValue();
            double signalValue = signal.getValue(i).doubleValue();
            double histValue = macdValue - signalValue;
            
            macdValues.add(macdValue);
            signalValues.add(signalValue);
            histogramValues.add(histValue);
        }
        
        Map<String, List<Double>> macdData = new HashMap<>();
        macdData.put("macd", macdValues);
        macdData.put("signal", signalValues);
        macdData.put("histogram", histogramValues);
        
        return macdData;
    }
    
    /**
     * 볼린저 밴드 계산
     */
    private Map<String, List<Double>> calculateBollingerBands(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        int period = 20;
        Num k = series.numOf(2.0);
        
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, period);
        
        BollingerBandsMiddleIndicator bbm = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator bbu = new BollingerBandsUpperIndicator(bbm, sd, k);
        BollingerBandsLowerIndicator bbl = new BollingerBandsLowerIndicator(bbm, sd, k);
        
        List<Double> upperValues = new ArrayList<>();
        List<Double> middleValues = new ArrayList<>();
        List<Double> lowerValues = new ArrayList<>();
        
        for (int i = 0; i < series.getBarCount(); i++) {
            // 초기 데이터가 부족한 구간
            if (i < period - 1) {
                upperValues.add(closePrice.getValue(i).doubleValue() * 1.05);
                middleValues.add(closePrice.getValue(i).doubleValue());
                lowerValues.add(closePrice.getValue(i).doubleValue() * 0.95);
            } else {
                upperValues.add(bbu.getValue(i).doubleValue());
                middleValues.add(bbm.getValue(i).doubleValue());
                lowerValues.add(bbl.getValue(i).doubleValue());
            }
        }
        
        Map<String, List<Double>> bbData = new HashMap<>();
        bbData.put("upper", upperValues);
        bbData.put("middle", middleValues);
        bbData.put("lower", lowerValues);
        
        return bbData;
    }
    
    /**
     * 최신 지표 값 추출
     */
    private Map<String, Double> extractLatestValues(Map<String, Object> indicators) {
        Map<String, Double> latestValues = new HashMap<>();
        
        // 처리할 지표 목록
        String[] indicatorKeys = {
            "sma20", "sma50", "sma200", "ema20", "rsi", 
            "macd", "macdSignal", "macdHist",
            "bbUpper", "bbMiddle", "bbLower"
        };
        
        for (String key : indicatorKeys) {
            if (indicators.containsKey(key)) {
                List<Double> values = (List<Double>) indicators.get(key);
                if (values != null && !values.isEmpty()) {
                    latestValues.put(key, values.get(values.size() - 1));
                }
            }
        }
        
        return latestValues;
    }
}