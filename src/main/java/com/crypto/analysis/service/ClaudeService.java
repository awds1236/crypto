package com.crypto.analysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            for (Map.Entry<String, Double> entry : indicators.entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(String.format("%.2f", entry.getValue())).append("\n");
            }
            
            prompt.append("\n공포/욕심 지수: ").append(((Map<String, Object>)data.get("fearGreedIndex")).get("value"))
                  .append(" (").append(((Map<String, Object>)data.get("fearGreedIndex")).get("valueClassification")).append(")\n\n");
            
            // 뉴스 데이터 추가
            prompt.append("최근 관련 뉴스 제목:\n");
            Map<String, Object> newsData = (Map<String, Object>) data.get("news");
            if (newsData != null && newsData.containsKey("news")) {
                List<JsonNode> newsItems = (List<JsonNode>) newsData.get("news");
                int newsCount = Math.min(newsItems.size(), 5); // 최대 5개 뉴스만 사용
                
                for (int i = 0; i < newsCount; i++) {
                    JsonNode news = newsItems.get(i);
                    prompt.append("- ").append(news.get("title").asText()).append("\n");
                }
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
            return "분석 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
