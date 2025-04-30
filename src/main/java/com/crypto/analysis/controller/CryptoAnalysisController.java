package com.crypto.analysis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.crypto.analysis.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Controller
public class CryptoAnalysisController {
    
    @Autowired
    private UpbitService upbitService;
    
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
    public String getMarkets() {
        return upbitService.getMarkets();
    }
    
    @GetMapping("/analyze")
    @ResponseBody
    public Map<String, Object> analyze(@RequestParam String market) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 캔들 데이터 조회
            String candleData = upbitService.getDayCandles(market, 100);
            
            // 기술적 지표 계산
            Map<String, Object> indicators = technicalIndicatorService.calculateAllIndicators(market, candleData);
            
            // 공포/욕심 지수 조회
            Map<String, Object> fearGreedIndex = marketSentimentService.getFearAndGreedIndex();
            
            // 관련 뉴스 조회
            String coinSymbol = market.split("-")[1]; // KRW-BTC에서 BTC 추출
            Map<String, Object> news = marketSentimentService.getNewsForCoin(coinSymbol);
            
            // 데이터 통합
            Map<String, Object> analysisData = new HashMap<>();
            analysisData.put("market", market);
            analysisData.put("technicalIndicators", indicators.get("latest"));
            analysisData.put("fearGreedIndex", fearGreedIndex);
            analysisData.put("news", news);
            
            // Claude API로 분석 요청
            String analysisResult = claudeService.generateAnalysis(analysisData);
            
            // 결과 반환
            result.put("success", true);
            result.put("analysis", analysisResult);
            result.put("indicators", indicators);
            result.put("fearGreedIndex", fearGreedIndex);
            result.put("news", news);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @GetMapping("/price")
    @ResponseBody
    public String getCurrentPrice(@RequestParam String market) {
        return upbitService.getCurrentPrice(market);
    }
    
    @GetMapping("/candles/day")
    @ResponseBody
    public String getDayCandles(@RequestParam String market, @RequestParam(defaultValue = "30") int count) {
        return upbitService.getDayCandles(market, count);
    }
    
    @GetMapping("/candles/hour")
    @ResponseBody
    public String getHourCandles(@RequestParam String market, @RequestParam(defaultValue = "24") int count) {
        return upbitService.getHourCandles(market, count);
    }
    
    @GetMapping("/candles/minute")
    @ResponseBody
    public String getMinuteCandles(
            @RequestParam String market, 
            @RequestParam(defaultValue = "1") int minutes,
            @RequestParam(defaultValue = "60") int count) {
        return upbitService.getMinuteCandles(market, minutes, count);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleError(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());
        return errorResponse;
    }
}
