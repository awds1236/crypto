package com.crypto.analysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class UpbitService {
    
    private final RestTemplate restTemplate;
    private final String API_URL = "https://api.upbit.com/v1";
    
    @Value("${upbit.api.access-key}")
    private String ACCESS_KEY;
    
    @Value("${upbit.api.secret-key}")
    private String SECRET_KEY;
    
    public UpbitService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // 시장 코인 목록 조회
    public String getMarkets() {
        String url = API_URL + "/market/all";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }
    
    // 특정 코인의 현재가 조회
    public String getCurrentPrice(String market) {
        String url = API_URL + "/ticker?markets=" + market;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }
    
    // 캔들 데이터 조회 (차트 데이터)
    public String getCandles(String market, String interval, int count) {
        String url = API_URL + "/candles/" + interval + "?market=" + market + "&count=" + count;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }
    
    // 일봉 캔들 조회
    public String getDayCandles(String market, int count) {
        return getCandles(market, "days", count);
    }
    
    // 시간봉 캔들 조회
    public String getHourCandles(String market, int count) {
        return getCandles(market, "minutes/60", count);
    }
    
    // 분봉 캔들 조회
    public String getMinuteCandles(String market, int minutes, int count) {
        return getCandles(market, "minutes/" + minutes, count);
    }
    
    // JWT 인증이 필요한 API 요청용 (입출금 내역 등)
    private String generateAuthenticationToken(Map<String, String> params) {
        try {
            String queryString = "";
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    queryString += param.getKey() + "=" + param.getValue() + "&";
                }
                queryString = queryString.substring(0, queryString.length() - 1);
            }
            
            String nonce = UUID.randomUUID().toString();
            String message = nonce + (queryString.equals("") ? "" : "?" + queryString);
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes()));
            
            String authToken = "Bearer " + ACCESS_KEY + ":" + signature + ":" + nonce;
            return authToken;
        } catch (Exception e) {
            throw new RuntimeException("인증 토큰 생성 실패", e);
        }
    }
    
    // 인증이 필요한 API 호출 예시
    public String getAccount() {
        String url = API_URL + "/accounts";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", generateAuthenticationToken(null));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        return response.getBody();
    }
}
