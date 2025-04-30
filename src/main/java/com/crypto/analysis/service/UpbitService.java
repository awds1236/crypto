package com.crypto.analysis.service;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
        try {
            String url = API_URL + "/market/all";
            System.out.println("Requesting URL: " + url);  // 로깅 추가
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            System.out.println("Response status: " + response.getStatusCode());  // 응답 상태 로깅
            return response.getBody();
        } catch (Exception e) {
            System.err.println("업비트 API 연결 실패, 임시 데이터를 사용합니다: " + e.getMessage());
            e.printStackTrace();
            return getFallbackMarkets();  // 실패 시 임시 데이터 반환
        }
    }
    
    // 업비트 API 호출에 실패했을 때 사용할 임시 데이터
    private String getFallbackMarkets() {
        return "[" +
                "{\"market\":\"KRW-BTC\",\"korean_name\":\"비트코인\",\"english_name\":\"Bitcoin\"}," +
                "{\"market\":\"KRW-ETH\",\"korean_name\":\"이더리움\",\"english_name\":\"Ethereum\"}," +
                "{\"market\":\"KRW-XRP\",\"korean_name\":\"리플\",\"english_name\":\"Ripple\"}," +
                "{\"market\":\"KRW-ADA\",\"korean_name\":\"에이다\",\"english_name\":\"Ada\"}," +
                "{\"market\":\"KRW-DOGE\",\"korean_name\":\"도지코인\",\"english_name\":\"Dogecoin\"}," +
                "{\"market\":\"KRW-SOL\",\"korean_name\":\"솔라나\",\"english_name\":\"Solana\"}," +
                "{\"market\":\"KRW-DOT\",\"korean_name\":\"폴카닷\",\"english_name\":\"Polkadot\"}," +
                "{\"market\":\"KRW-AVAX\",\"korean_name\":\"아발란체\",\"english_name\":\"Avalanche\"}," +
                "{\"market\":\"KRW-MATIC\",\"korean_name\":\"폴리곤\",\"english_name\":\"Polygon\"}," +
                "{\"market\":\"KRW-LINK\",\"korean_name\":\"체인링크\",\"english_name\":\"Chainlink\"}," +
                "{\"market\":\"KRW-UNI\",\"korean_name\":\"유니스왑\",\"english_name\":\"Uniswap\"}," +
                "{\"market\":\"KRW-ATOM\",\"korean_name\":\"코스모스\",\"english_name\":\"Cosmos\"}," +
                "{\"market\":\"KRW-AAVE\",\"korean_name\":\"에이브\",\"english_name\":\"Aave\"}," +
                "{\"market\":\"KRW-ALGO\",\"korean_name\":\"알고랜드\",\"english_name\":\"Algorand\"}," +
                "{\"market\":\"KRW-XLM\",\"korean_name\":\"스텔라루멘\",\"english_name\":\"Stellar Lumens\"}," +
                "{\"market\":\"KRW-ETC\",\"korean_name\":\"이더리움클래식\",\"english_name\":\"Ethereum Classic\"}," +
                "{\"market\":\"KRW-NEAR\",\"korean_name\":\"니어프로토콜\",\"english_name\":\"NEAR Protocol\"}," +
                "{\"market\":\"KRW-SHIB\",\"korean_name\":\"시바이누\",\"english_name\":\"Shiba Inu\"}," +
                "{\"market\":\"KRW-SAND\",\"korean_name\":\"샌드박스\",\"english_name\":\"The Sandbox\"}," +
                "{\"market\":\"KRW-1INCH\",\"korean_name\":\"1인치네트워크\",\"english_name\":\"1inch Network\"}" +
                "]";
    }
    
    // 특정 코인의 현재가 조회
    public String getCurrentPrice(String market) {
        try {
            String url = API_URL + "/ticker?markets=" + market;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("현재가 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return "[{\"market\":\"" + market + "\",\"trade_price\":0,\"change_rate\":0}]";
        }
    }
    
    // 캔들 데이터 조회 (차트 데이터)
    public String getCandles(String market, String interval, int count) {
        try {
            String url = API_URL + "/candles/" + interval + "?market=" + market + "&count=" + count;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("캔들 데이터 조회 실패: " + e.getMessage());
            e.printStackTrace();
            // 임시 캔들 데이터 생성 (실패 시)
            return generateFallbackCandles(market, count);
        }
    }
    
    // 임시 캔들 데이터 생성
    private String generateFallbackCandles(String market, int count) {
        StringBuilder sb = new StringBuilder("[");
        long now = System.currentTimeMillis();
        double basePrice = 50000000; // 기본 가격 (예: 비트코인)
        
        if (market.equals("KRW-ETH")) basePrice = 3000000;
        else if (market.equals("KRW-XRP")) basePrice = 500;
        else if (!market.equals("KRW-BTC")) basePrice = 10000;
        
        for (int i = 0; i < count; i++) {
            double fluctuation = 0.02 * Math.random() - 0.01; // -1% ~ +1% 변동
            double price = basePrice * (1 + fluctuation);
            
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"market\":\"").append(market).append("\",");
            sb.append("\"candle_date_time_utc\":\"").append(java.time.Instant.ofEpochMilli(now - (i * 86400000)).toString().replace("Z", "")).append("\",");
            sb.append("\"candle_date_time_kst\":\"").append(java.time.Instant.ofEpochMilli(now - (i * 86400000) + 32400000).toString().replace("Z", "")).append("\",");
            sb.append("\"opening_price\":").append(price * 0.99).append(",");
            sb.append("\"high_price\":").append(price * 1.02).append(",");
            sb.append("\"low_price\":").append(price * 0.98).append(",");
            sb.append("\"trade_price\":").append(price).append(",");
            sb.append("\"candle_acc_trade_price\":").append(price * 100).append(",");
            sb.append("\"candle_acc_trade_volume\":").append(100 + Math.random() * 50);
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
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