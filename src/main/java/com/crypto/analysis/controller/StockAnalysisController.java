package com.crypto.analysis.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.crypto.analysis.model.Stock;
import com.crypto.analysis.service.ClaudeService;
import com.crypto.analysis.service.StockService;
import com.crypto.analysis.service.TechnicalIndicatorService;

@RestController
@RequestMapping("/api/stock")
public class StockAnalysisController {
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ClaudeService claudeService;
    
    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;
    
    /**
     * 상위 거래량 30개 종목 조회
     */
    @GetMapping("/top-volume")
    @ResponseBody
    public ResponseEntity<?> getTopVolumeStocks(
            @RequestParam(defaultValue = "US") String market,
            @RequestParam(defaultValue = "30") int limit) {
        try {
            System.out.println("상위 거래량 종목 요청: 시장=" + market + ", 개수=" + limit);
            List<Stock> stocks = stockService.getTopVolumeStocks(limit);
            
            // 지정된 시장에 해당하는 종목만 필터링
            if (!market.equals("ALL")) {
                stocks = stocks.stream()
                    .filter(stock -> market.equalsIgnoreCase(stock.getExchange()))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            System.out.println("응답 종목 수: " + stocks.size());
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            System.err.println("상위 종목 조회 오류: " + e.getMessage());
            e.printStackTrace();
            
            // 에러 응답 전송
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "상위 종목 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 종목 검색
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchStocks(
            @RequestParam String query, 
            @RequestParam(defaultValue = "US") String market) {
        try {
            System.out.println("종목 검색 요청: 검색어=" + query + ", 시장=" + market);
            List<Stock> stocks = stockService.searchStocks(query, market);
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            System.err.println("종목 검색 오류: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "종목 검색 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 종목 분석
     */
    @GetMapping("/analyze")
    @ResponseBody
    public ResponseEntity<?> analyzeStock(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "US") String market) {
        System.out.println("종목 분석 요청: 심볼=" + symbol + ", 시장=" + market);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 주식 데이터 가져오기
            Map<String, Object> stockData = stockService.getStockData(symbol, market);
            
            // 히스토리컬 데이터 가져오기
            String historicalData = stockService.getHistoricalData(symbol, market, 30);
            
            // 캔들 데이터 변환
            stockData.put("candles", historicalData);
            
            // 기술적 지표 계산
            Map<String, Object> indicators = technicalIndicatorService.calculateStockIndicators(symbol, historicalData);
            
            // 뉴스 데이터 가져오기
            Map<String, Object> newsData = stockService.getNewsForStock(symbol);
            
            // 재무 데이터 가져오기
            Map<String, Object> financials = stockService.getFinancialData(symbol, market);
            
            // 통합 데이터
            Map<String, Object> analysisData = new HashMap<>();
            analysisData.put("symbol", symbol);
            analysisData.put("market", market);
            analysisData.put("companyName", stockData.get("companyName"));
            analysisData.put("currentPrice", stockData.get("currentPrice"));
            analysisData.put("historicalData", historicalData);
            analysisData.put("technicalIndicators", indicators.get("latest"));
            analysisData.put("financials", financials);
            analysisData.put("news", newsData);
            
            // Claude API로 분석 요청
            String analysisResult = claudeService.generateStockAnalysis(analysisData);
            
            // JSON 응답에서 추출 (필요시)
            String jsonResponse = extractJsonFromResponse(analysisResult);
            
            // 분석 결과
            result.put("success", true);
            result.put("symbol", symbol);
            result.put("companyName", stockData.get("companyName"));
            result.put("currentPrice", stockData.get("currentPrice"));
            result.put("changePercent", stockData.get("changePercent"));
            result.put("analysis", jsonResponse);
            result.put("rawAnalysis", analysisResult);
            result.put("indicators", indicators);
            result.put("financials", financials);
            result.put("news", newsData);
            
            // 차트 데이터 추가
            result.put("dates", indicators.get("dates"));
            result.put("prices", indicators.get("prices"));
            result.put("volumes", indicators.get("volumes"));
            
            System.out.println("분석 완료: " + symbol);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("종목 분석 오류: " + e.getMessage());
            e.printStackTrace();
            
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
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
}