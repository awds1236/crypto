package com.crypto.analysis.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.crypto.analysis.service.BinanceService;
import com.crypto.analysis.service.ClaudeService;
import com.crypto.analysis.service.MarketSentimentService;
import com.crypto.analysis.service.TechnicalIndicatorService;
import com.crypto.analysis.service.UpbitService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class CryptoAnalysisController {
    
    @Autowired
    private UpbitService upbitService;
    
    @Autowired
    private BinanceService binanceService;
    
    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;
    
    @Autowired
    private MarketSentimentService marketSentimentService;
    
    @Autowired
    private ClaudeService claudeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @GetMapping("/")
    public String home(Model model) {
        try {
            return "index";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/markets")
    @ResponseBody
    public String getMarkets(@RequestParam(defaultValue = "upbit") String exchange) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getMarkets();
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getSymbols();
            } else {
                return "[]";
            }
        } catch (Exception e) {
            System.err.println("Error fetching coin list: " + e.getMessage());
            return "[]";
        }
    }
    
    @GetMapping("/analyze")
    @ResponseBody
    public Map<String, Object> analyze(
            @RequestParam String market,
            @RequestParam(defaultValue = "upbit") String exchange) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String candleData, currentPrice;
            
            // 거래소에 따라 API 호출 서비스 선택
            if ("upbit".equalsIgnoreCase(exchange)) {
                // 캔들 데이터 조회
                candleData = upbitService.getDayCandles(market, 30);
                
                // 현재가 조회
                currentPrice = upbitService.getCurrentPrice(market);
            } else if ("binance".equalsIgnoreCase(exchange)) {
                // 캔들 데이터 조회
                candleData = binanceService.getDayCandles(market, 30);
                
                // 현재가 조회
                currentPrice = binanceService.getCurrentPrice(market);
            } else {
                result.put("success", false);
                result.put("error", "지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.");
                return result;
            }
            
            // 기술적 지표 계산
            Map<String, Object> indicators = technicalIndicatorService.calculateAllIndicators(market, candleData);
            
            // 공포/욕심 지수 조회
            Map<String, Object> fearGreedIndex = marketSentimentService.getFearAndGreedIndex();
            
            // 관련 뉴스 조회
            String coinSymbol;
            if ("upbit".equalsIgnoreCase(exchange)) {
                coinSymbol = market.split("-")[1]; // KRW-BTC에서 BTC 추출
            } else {
                coinSymbol = market.replace("USDT", ""); // BTCUSDT에서 BTC 추출
            }
            Map<String, Object> news = marketSentimentService.getNewsForCoin(coinSymbol);
            
            // 데이터 통합
            Map<String, Object> analysisData = new HashMap<>();
            analysisData.put("market", market);
            analysisData.put("exchange", exchange);
            analysisData.put("currentPrice", currentPrice);
            analysisData.put("candles", candleData);
            analysisData.put("technicalIndicators", indicators.get("latest"));
            analysisData.put("fearGreedIndex", fearGreedIndex);
            analysisData.put("news", news);
            analysisData.put("sentimentService", marketSentimentService); // 감성 분석을 위해 서비스 전달
            
            // Claude API로 분석 요청
            String analysisResult = claudeService.generateAnalysis(analysisData);
            
            // JSON 응답에서 추출
            String jsonResponse = extractJsonFromResponse(analysisResult);
            
            // 결과 반환
            result.put("success", true);
            result.put("analysis", jsonResponse);
            result.put("rawAnalysis", analysisResult); // 원본 분석 텍스트도 함께 전달
            result.put("indicators", indicators);
            result.put("fearGreedIndex", fearGreedIndex);
            result.put("news", news);
            result.put("exchange", exchange); // 거래소 정보 추가
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Claude 응답에서 JSON 부분만 추출
     */
    private String extractJsonFromResponse(String response) {
        try {
            int startIndex = response.indexOf("```json");
            int endIndex = response.lastIndexOf("```");
            
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                // json 블록 시작 부분 이후부터 추출
                startIndex = response.indexOf("\n", startIndex) + 1;
                return response.substring(startIndex, endIndex).trim();
            }
            
            // JSON 형식이 아닌 경우 전체 응답 반환
            return response;
        } catch (Exception e) {
            System.err.println("JSON 추출 실패: " + e.getMessage());
            return response;
        }
    }
    
    @GetMapping("/price")
    @ResponseBody
    public String getCurrentPrice(
            @RequestParam String market,
            @RequestParam(defaultValue = "upbit") String exchange) {
        if ("upbit".equalsIgnoreCase(exchange)) {
            return upbitService.getCurrentPrice(market);
        } else if ("binance".equalsIgnoreCase(exchange)) {
            return binanceService.getCurrentPrice(market);
        } else {
            return "{\"error\":\"지원하지 않는 거래소입니다.\"}";
        }
    }
    
    @GetMapping("/candles/day")
    @ResponseBody
    public String getDayCandles(
            @RequestParam String market, 
            @RequestParam(defaultValue = "30") int count,
            @RequestParam(defaultValue = "upbit") String exchange) {
        if ("upbit".equalsIgnoreCase(exchange)) {
            return upbitService.getDayCandles(market, count);
        } else if ("binance".equalsIgnoreCase(exchange)) {
            return binanceService.getDayCandles(market, count);
        } else {
            return "{\"error\":\"지원하지 않는 거래소입니다.\"}";
        }
    }
    
    @GetMapping("/candles/hour")
    @ResponseBody
    public String getHourCandles(
            @RequestParam String market, 
            @RequestParam(defaultValue = "24") int count,
            @RequestParam(defaultValue = "upbit") String exchange) {
        if ("upbit".equalsIgnoreCase(exchange)) {
            return upbitService.getHourCandles(market, count);
        } else if ("binance".equalsIgnoreCase(exchange)) {
            return binanceService.getHourCandles(market, count);
        } else {
            return "{\"error\":\"지원하지 않는 거래소입니다.\"}";
        }
    }
    
    @GetMapping("/candles/minute")
    @ResponseBody
    public String getMinuteCandles(
            @RequestParam String market, 
            @RequestParam(defaultValue = "1") int minutes,
            @RequestParam(defaultValue = "60") int count,
            @RequestParam(defaultValue = "upbit") String exchange) {
        if ("upbit".equalsIgnoreCase(exchange)) {
            return upbitService.getMinuteCandles(market, minutes, count);
        } else if ("binance".equalsIgnoreCase(exchange)) {
            return binanceService.getMinuteCandles(market, minutes, count);
        } else {
            return "{\"error\":\"지원하지 않는 거래소입니다.\"}";
        }
    }
    
    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        model.addAttribute("message", "서비스 처리 중 오류가 발생했습니다.");
        model.addAttribute("error", e.getMessage());
        return "error";
    }
}