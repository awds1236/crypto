package com.crypto.analysis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ClaudeService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String API_URL = "https://api.anthropic.com/v1/messages";
    
    @Value("${claude.api.key}")
    private String API_KEY;
    
    public ClaudeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateAnalysis(Map<String, Object> data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", API_KEY);
            headers.set("anthropic-version", "2023-06-01");
            
            // 프롬프트 구성
            StringBuilder prompt = new StringBuilder();
            prompt.append("가상화폐 ")
                  .append(data.get("market"))
                  .append("에 대한 기술적 분석을 해주세요. 다음은 관련 데이터입니다:\n\n");
            
            prompt.append("기술적 지표:\n");
            Map<String, Double> indicators = (Map<String, Double>) data.get("technicalIndicators");
            if (indicators != null) {
                for (Map.Entry<String, Double> entry : indicators.entrySet()) {
                    prompt.append("- ").append(entry.getKey()).append(": ").append(String.format("%.2f", entry.getValue())).append("\n");
                }
            } else {
                prompt.append("- 기술적 지표 데이터를 불러올 수 없습니다.\n");
            }
            
            // 공포/욕심 지수 정보 추가
            Map<String, Object> fearGreedIndex = (Map<String, Object>) data.get("fearGreedIndex");
            if (fearGreedIndex != null) {
                prompt.append("\n공포/욕심 지수: ").append(fearGreedIndex.get("value"))
                      .append(" (").append(fearGreedIndex.get("valueClassification")).append(")\n\n");
            } else {
                prompt.append("\n공포/욕심 지수: 데이터를 불러올 수 없습니다.\n\n");
            }
            
            // 뉴스 데이터 추가
            prompt.append("최근 관련 뉴스 제목:\n");
            Map<String, Object> newsData = (Map<String, Object>) data.get("news");
            if (newsData != null && newsData.containsKey("news")) {
                List<JsonNode> newsItems = (List<JsonNode>) newsData.get("news");
                int newsCount = Math.min(newsItems.size(), 5); // 최대 5개 뉴스만 사용
                
                if (newsCount > 0) {
                    for (int i = 0; i < newsCount; i++) {
                        JsonNode news = newsItems.get(i);
                        prompt.append("- ").append(news.get("title").asText()).append("\n");
                    }
                } else {
                    prompt.append("- 관련 뉴스를 찾을 수 없습니다.\n");
                }
            } else {
                prompt.append("- 뉴스 데이터를 불러올 수 없습니다.\n");
            }
            
            prompt.append("\n이 데이터를 바탕으로 다음 질문에 답해주세요:\n");
            prompt.append("1. 현재 가격 추세는 어떠한가요? (상승/하락/횡보)\n");
            prompt.append("2. 기술적 지표들이 어떤 신호를 보내고 있나요?\n");
            prompt.append("3. 매수, 매도, 홀딩 중 어떤 전략이 적합한가요?\n");
            prompt.append("4. 단기(1-3일), 중기(1-2주), 장기(1개월 이상) 전망은 어떤가요?\n");
            prompt.append("5. 투자자들이 주의해야 할 점은 무엇인가요?\n\n");
            prompt.append("각 답변은 간결하고 명확하게 설명해주세요. 분석 결과는 투자 조언이 아닌 참고용임을 명시해주세요.");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-3-opus-20240229");
            requestBody.put("max_tokens", 2000);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt.toString());
            messages.add(message);
            
            requestBody.put("messages", messages);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            Map<String, Object> response = restTemplate.postForObject(API_URL, request, Map.class);
            Map<String, Object> responseContent = (Map<String, Object>) ((List<Object>) response.get("content")).get(0);
            return (String) responseContent.get("text");
            
        } catch (Exception e) {
            e.printStackTrace();
            
            // 오류 시 대체 분석 결과 제공
            return generateFallbackAnalysis(data);
        }
    }
    
    /**
     * Claude API 호출 실패 시 대체 분석 결과 생성
     */
    private String generateFallbackAnalysis(Map<String, Object> data) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("# 코인 분석 결과\n\n");
        
        String market = data.get("market").toString();
        fallback.append("현재 **").append(market).append("**에 대한 분석입니다. (Claude API 연결이 원활하지 않아 기본 분석만 제공됩니다.)\n\n");
        
        fallback.append("## 기술적 지표 분석\n\n");
        
        Map<String, Double> indicators = (Map<String, Double>) data.get("technicalIndicators");
        if (indicators != null) {
            double rsi = indicators.getOrDefault("rsi14", 50.0);
            
            if (rsi > 70) {
                fallback.append("RSI(14)가 ").append(String.format("%.2f", rsi)).append("로 **과매수** 상태입니다. 단기적으로 하락 조정 가능성이 있습니다.\n\n");
            } else if (rsi < 30) {
                fallback.append("RSI(14)가 ").append(String.format("%.2f", rsi)).append("로 **과매도** 상태입니다. 단기적인 반등 가능성이 있습니다.\n\n");
            } else {
                fallback.append("RSI(14)가 ").append(String.format("%.2f", rsi)).append("로 중립적인 범위에 있습니다.\n\n");
            }
            
            double sma20 = indicators.getOrDefault("sma20", 0.0);
            double ema20 = indicators.getOrDefault("ema20", 0.0);
            
            if (ema20 > sma20) {
                fallback.append("EMA(20)가 SMA(20)보다 높은 상황으로 상승 추세의 가능성이 있습니다.\n\n");
            } else if (ema20 < sma20) {
                fallback.append("EMA(20)가 SMA(20)보다 낮은 상황으로 하락 추세의 가능성이 있습니다.\n\n");
            } else {
                fallback.append("EMA(20)와 SMA(20)가 비슷한 수준으로 뚜렷한 추세가 나타나지 않고 있습니다.\n\n");
            }
        }
        
        fallback.append("## 시장 심리\n\n");
        Map<String, Object> fearGreedIndex = (Map<String, Object>) data.get("fearGreedIndex");
        if (fearGreedIndex != null) {
            int value = 0;
            if (fearGreedIndex.get("value") instanceof Integer) {
                value = (Integer) fearGreedIndex.get("value");
            } else if (fearGreedIndex.get("value") instanceof String) {
                value = Integer.parseInt((String) fearGreedIndex.get("value"));
            }
            
            String classification = (String) fearGreedIndex.get("valueClassification");
            
            fallback.append("현재 공포/욕심 지수는 **").append(value).append("(").append(classification).append(")**입니다. ");
            
            if (value < 25) {
                fallback.append("시장이 극도의 공포 상태로, 매수 기회가 될 수 있지만 추가 하락에 주의해야 합니다.\n\n");
            } else if (value < 40) {
                fallback.append("시장이 공포 상태로, 조심스러운 매수를 고려해볼 수 있습니다.\n\n");
            } else if (value < 60) {
                fallback.append("시장이 중립적인 상태로, 현재 포지션 유지가 적절할 수 있습니다.\n\n");
            } else if (value < 80) {
                fallback.append("시장이 욕심 상태로, 부분적인 이익실현을 고려해볼 수 있습니다.\n\n");
            } else {
                fallback.append("시장이 극도의 욕심 상태로, 고점에 가까울 가능성이 있어 주의가 필요합니다.\n\n");
            }
        }
        
        fallback.append("## 투자 유의사항\n\n");
        fallback.append("* 가상화폐 시장은 변동성이 매우 큽니다.\n");
        fallback.append("* 본 분석은 참고용이며 투자 결정은 스스로 내려야 합니다.\n");
        fallback.append("* 투자 원금의 손실 가능성이 항상 존재함을 유념하세요.\n");
        fallback.append("* 여러 지표와 뉴스를 종합적으로 판단하는 것이 중요합니다.\n\n");
        
        fallback.append("추가적인 분석이 필요할 경우 다시 시도해주세요.");
        
        return fallback.toString();
    }
}