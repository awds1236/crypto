package com.crypto.analysis.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MarketSentimentService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public MarketSentimentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    // 공포/욕심 지수 가져오기 (Alternative.me API 사용)
    public Map<String, Object> getFearAndGreedIndex() throws Exception {
        String url = "https://api.alternative.me/fng/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode responseData = objectMapper.readTree(response.getBody());
        
        Map<String, Object> sentimentData = new HashMap<>();
        sentimentData.put("value", responseData.get("data").get(0).get("value").asInt());
        sentimentData.put("valueClassification", responseData.get("data").get(0).get("value_classification").asText());
        
        return sentimentData;
    }
    
    // 가상화폐 뉴스 가져오기 (CryptoCompare API 사용)
    public Map<String, Object> getCryptoNews() throws Exception {
        String url = "https://min-api.cryptocompare.com/data/v2/news/?lang=EN";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode newsData = objectMapper.readTree(response.getBody());
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", newsData.get("Data"));
        
        return result;
    }
    
    // 특정 코인에 대한 뉴스 필터링
    public Map<String, Object> getNewsForCoin(String coinSymbol) throws Exception {
        Map<String, Object> allNews = getCryptoNews();
        JsonNode newsData = (JsonNode) allNews.get("data");
        
        List<JsonNode> filteredNews = new java.util.ArrayList<>();
        
        for (JsonNode news : newsData) {
            String categories = news.get("categories").asText();
            String title = news.get("title").asText();
            String body = news.get("body").asText();
            
            // 코인 심볼이 카테고리, 제목 또는 본문에 포함되어 있는지 확인
            if (categories.contains(coinSymbol) || 
                title.contains(coinSymbol) || 
                body.contains(coinSymbol)) {
                filteredNews.add(news);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("coin", coinSymbol);
        result.put("newsCount", filteredNews.size());
        result.put("news", filteredNews);
        
        return result;
    }
    
    // 감성 분석 점수 계산 (간단한 키워드 기반 구현)
    public double calculateSentimentScore(Map<String, Object> news) {
        List<JsonNode> newsItems = (List<JsonNode>) news.get("news");
        
        // 긍정적, 부정적 키워드 정의
        String[] positiveWords = {"상승", "성장", "호재", "채택", "개선", "기회", 
                               "bull", "bullish", "surge", "soar", "gain", "rally", "positive"};
        String[] negativeWords = {"하락", "감소", "악재", "규제", "제한", "위험", 
                               "bear", "bearish", "crash", "plunge", "drop", "fall", "negative"};
        
        double totalScore = 0;
        
        for (JsonNode item : newsItems) {
            String title = item.get("title").asText().toLowerCase();
            String body = item.get("body").asText().toLowerCase();
            String fullText = title + " " + body;
            
            double itemScore = 0;
            
            // 긍정 키워드 확인
            for (String word : positiveWords) {
                if (fullText.contains(word.toLowerCase())) {
                    itemScore += 1;
                }
            }
            
            // 부정 키워드 확인
            for (String word : negativeWords) {
                if (fullText.contains(word.toLowerCase())) {
                    itemScore -= 1;
                }
            }
            
            totalScore += itemScore;
        }
        
        // 뉴스 항목 수로 정규화 (항목이 없으면 중립 0 반환)
        if (newsItems.size() > 0) {
            return totalScore / newsItems.size();
        } else {
            return 0;
        }
    }
}
