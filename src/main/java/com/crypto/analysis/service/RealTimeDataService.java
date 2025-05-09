package com.crypto.analysis.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RealTimeDataService {
    
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public RealTimeDataService(RestTemplate restTemplate) {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplate;
    }
    
    public String getTickerData(List<String> markets) {
        try {
            // 마켓 목록을 콤마로 구분된 문자열로 변환
            String marketsParam = String.join(",", markets);
            
            // 업비트 API로 현재가 정보 요청
            String url = "https://api.upbit.com/v1/ticker?markets=" + marketsParam;
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Crypto-Analysis-Application");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            // 응답 데이터 반환
            return response.getBody();
        } catch (Exception e) {
            System.err.println("티커 데이터 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return "[]"; // 오류 시 빈 배열 반환
        }
    }
    
    // 이전 WebSocket 관련 코드는 삭제
    
    // shutdown 메서드는 유지 (비어있는 상태로)
    public void shutdown() {
        // 리소스 해제가 필요 없으므로 비워둡니다
    }

    // 메시지 전송 메서드 (이전 subscribeToTickerData 대신 사용)
    public void subscribeToTickerData(List<String> markets) {
        try {
            // 티커 데이터 가져오기
            String tickerData = getTickerData(markets);
            
            // 클라이언트에 데이터 전송
            messagingTemplate.convertAndSend("/topic/ticker", tickerData);
            
            System.out.println("티커 데이터 전송 완료");
        } catch (Exception e) {
            System.err.println("티커 데이터 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}