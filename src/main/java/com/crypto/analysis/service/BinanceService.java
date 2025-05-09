package com.crypto.analysis.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class BinanceService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String API_URL = "https://api.binance.com/api/v3";
    
    @Value("${binance.api.key:}")
    private String API_KEY;
    
    @Value("${binance.api.secret:}")
    private String API_SECRET;
    
    public BinanceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 바이낸스 심볼 목록 가져오기
     */
    public String getSymbols() {
        try {
            String url = API_URL + "/exchangeInfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Crypto-Analysis-Application");
            
            if (!API_KEY.isEmpty()) {
                headers.set("X-MBX-APIKEY", API_KEY);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            // 응답에서 심볼 정보만 필터링하여 반환
            String responseBody = response.getBody();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode symbols = root.get("symbols");
            
            // USDT 마켓만 필터링하고 필요한 정보만 추출
            ArrayNode filteredSymbols = objectMapper.createArrayNode();
            for (JsonNode symbol : symbols) {
                String symbolName = symbol.get("symbol").asText();
                String baseAsset = symbol.get("baseAsset").asText();
                String quoteAsset = symbol.get("quoteAsset").asText();
                
                // USDT 마켓만 필터링
                if (quoteAsset.equals("USDT")) {
                    JsonNode filteredSymbol = objectMapper.createObjectNode()
                        .put("market", symbolName)
                        .put("baseAsset", baseAsset)
                        .put("quoteAsset", quoteAsset)
                        .put("korean_name", getKoreanName(baseAsset))
                        .put("english_name", getEnglishName(baseAsset));
                        
                    filteredSymbols.add(filteredSymbol);
                }
            }
            
            return objectMapper.writeValueAsString(filteredSymbols);
        } catch (Exception e) {
            System.err.println("바이낸스 API 연결 실패: " + e.getMessage());
            e.printStackTrace();
            return getFallbackSymbols();
        }
    }
    
    /**
     * 바이낸스 API 호출 실패 시 사용할 기본 심볼 데이터
     */
    private String getFallbackSymbols() {
        try {
            ArrayNode symbols = objectMapper.createArrayNode();
            
            // 주요 암호화폐에 대한 기본 데이터 생성
            String[][] defaultCoins = {
                {"BTCUSDT", "비트코인", "Bitcoin"},
                {"ETHUSDT", "이더리움", "Ethereum"},
                {"XRPUSDT", "리플", "Ripple"},
                {"ADAUSDT", "에이다", "Cardano"},
                {"DOGEUSDT", "도지코인", "Dogecoin"},
                {"SOLUSDT", "솔라나", "Solana"},
                {"DOTUSDT", "폴카닷", "Polkadot"},
                {"AVAXUSDT", "아발란체", "Avalanche"},
                {"MATICUSDT", "폴리곤", "Polygon"},
                {"LINKUSDT", "체인링크", "Chainlink"}
            };
            
            for (String[] coin : defaultCoins) {
                String market = coin[0];
                String baseAsset = market.replace("USDT", "");
                
                JsonNode symbol = objectMapper.createObjectNode()
                    .put("market", market)
                    .put("baseAsset", baseAsset)
                    .put("quoteAsset", "USDT")
                    .put("korean_name", coin[1])
                    .put("english_name", coin[2]);
                    
                symbols.add(symbol);
            }
            
            return objectMapper.writeValueAsString(symbols);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
    
    /**
     * 특정 심볼의 현재가 조회
     */
    public String getCurrentPrice(String symbol) {
        try {
            String url = API_URL + "/ticker/price?symbol=" + symbol;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            // 업비트 API 형식과 유사하게 반환 형식 변환
            JsonNode priceData = objectMapper.readTree(response.getBody());
            String price = priceData.get("price").asText();
            
            // 24시간 가격 변화율 조회
            String changeUrl = API_URL + "/ticker/24hr?symbol=" + symbol;
            ResponseEntity<String> changeResponse = restTemplate.exchange(
                changeUrl, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            JsonNode changeData = objectMapper.readTree(changeResponse.getBody());
            String priceChangePercent = changeData.get("priceChangePercent").asText();
            
            // 결과 포맷팅
            String result = "[{\"market\":\"" + symbol + "\",\"code\":\"" + symbol + "\","
                + "\"trade_price\":" + price + ","
                + "\"change_rate\":" + (Double.parseDouble(priceChangePercent) / 100) + "}]";
                
            return result;
        } catch (Exception e) {
            System.err.println("현재가 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return "[{\"market\":\"" + symbol + "\",\"code\":\"" + symbol + "\",\"trade_price\":0,\"change_rate\":0}]";
        }
    }
    
    /**
     * 캔들 데이터 조회
     */
    public String getCandles(String symbol, String interval, int limit) {
        try {
            String url = API_URL + "/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            // 바이낸스 API 응답을 업비트 API 형식으로 변환
            JsonNode candlesData = objectMapper.readTree(response.getBody());
            ArrayNode formattedCandles = objectMapper.createArrayNode();
            
            for (JsonNode candle : candlesData) {
                long openTime = candle.get(0).asLong(); // 시가 시간 (밀리초)
                String openPrice = candle.get(1).asText(); // 시가
                String highPrice = candle.get(2).asText(); // 고가
                String lowPrice = candle.get(3).asText(); // 저가
                String closePrice = candle.get(4).asText(); // 종가
                String volume = candle.get(5).asText(); // 거래량
                
                // 날짜 변환 (UTC)
                java.time.Instant instant = java.time.Instant.ofEpochMilli(openTime);
                String utcTimeStr = instant.toString().replace("Z", "");
                
                // KST 시간 변환 (UTC + 9시간)
                java.time.ZonedDateTime kstTime = java.time.ZonedDateTime.ofInstant(
                    instant, java.time.ZoneId.of("Asia/Seoul"));
                String kstTimeStr = kstTime.toLocalDateTime().toString();
                
                // 업비트 API 형식에 맞게 변환
                JsonNode formattedCandle = objectMapper.createObjectNode()
                    .put("market", symbol)
                    .put("candle_date_time_utc", utcTimeStr)
                    .put("candle_date_time_kst", kstTimeStr)
                    .put("opening_price", Double.parseDouble(openPrice))
                    .put("high_price", Double.parseDouble(highPrice))
                    .put("low_price", Double.parseDouble(lowPrice))
                    .put("trade_price", Double.parseDouble(closePrice))
                    .put("candle_acc_trade_price", Double.parseDouble(volume) * Double.parseDouble(closePrice)) // 대략적인 계산
                    .put("candle_acc_trade_volume", Double.parseDouble(volume));
                    
                formattedCandles.add(formattedCandle);
            }
            
            return objectMapper.writeValueAsString(formattedCandles);
        } catch (Exception e) {
            System.err.println("캔들 데이터 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackCandles(symbol, limit);
        }
    }
    
    /**
     * 임시 캔들 데이터 생성
     */
    private String generateFallbackCandles(String symbol, int count) {
        try {
            ArrayNode candles = objectMapper.createArrayNode();
            long now = System.currentTimeMillis();
            double basePrice = 50000.0; // 기본 가격 (예: 비트코인)
            
            if (symbol.equals("ETHUSDT")) basePrice = 3000.0;
            else if (symbol.equals("XRPUSDT")) basePrice = 0.5;
            else if (symbol.equals("SOLUSDT")) basePrice = 100.0;
            else if (!symbol.equals("BTCUSDT")) basePrice = 10.0;
            
            for (int i = count - 1; i >= 0; i--) {
                double fluctuation = 0.02 * Math.random() - 0.01; // -1% ~ +1% 변동
                double price = basePrice * (1 + fluctuation * i);
                long timestamp = now - (i * 86400000); // 하루 간격
                
                // 날짜 변환
                java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
                String utcTimeStr = instant.toString().replace("Z", "");
                
                // KST 시간 변환 (UTC + 9시간)
                java.time.ZonedDateTime kstTime = java.time.ZonedDateTime.ofInstant(
                    instant, java.time.ZoneId.of("Asia/Seoul"));
                String kstTimeStr = kstTime.toLocalDateTime().toString();
                
                JsonNode candle = objectMapper.createObjectNode()
                    .put("market", symbol)
                    .put("candle_date_time_utc", utcTimeStr)
                    .put("candle_date_time_kst", kstTimeStr)
                    .put("opening_price", price * 0.99)
                    .put("high_price", price * 1.02)
                    .put("low_price", price * 0.98)
                    .put("trade_price", price)
                    .put("candle_acc_trade_price", price * 100)
                    .put("candle_acc_trade_volume", 100 + Math.random() * 50);
                    
                candles.add(candle);
            }
            
            return objectMapper.writeValueAsString(candles);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
    
    /**
     * 일봉 캔들 조회
     */
    public String getDayCandles(String symbol, int count) {
        return getCandles(symbol, "1d", count);
    }
    
    /**
     * 시간봉 캔들 조회
     */
    public String getHourCandles(String symbol, int count) {
        return getCandles(symbol, "1h", count);
    }
    
    /**
     * 분봉 캔들 조회
     */
    public String getMinuteCandles(String symbol, int minutes, int count) {
        String interval;
        if (minutes == 1) interval = "1m";
        else if (minutes == 3) interval = "3m";
        else if (minutes == 5) interval = "5m";
        else if (minutes == 15) interval = "15m";
        else if (minutes == 30) interval = "30m";
        else interval = "1h"; // 기본값
        
        return getCandles(symbol, interval, count);
    }
    
    /**
     * 심볼에 해당하는 한글 이름 반환
     */
    private String getKoreanName(String symbol) {
        Map<String, String> koreanNames = new HashMap<>();
        koreanNames.put("BTC", "비트코인");
        koreanNames.put("ETH", "이더리움");
        koreanNames.put("XRP", "리플");
        koreanNames.put("ADA", "에이다");
        koreanNames.put("DOGE", "도지코인");
        koreanNames.put("SOL", "솔라나");
        koreanNames.put("DOT", "폴카닷");
        koreanNames.put("AVAX", "아발란체");
        koreanNames.put("MATIC", "폴리곤");
        koreanNames.put("LINK", "체인링크");
        koreanNames.put("UNI", "유니스왑");
        koreanNames.put("ATOM", "코스모스");
        koreanNames.put("AAVE", "에이브");
        koreanNames.put("ALGO", "알고랜드");
        koreanNames.put("XLM", "스텔라루멘");
        koreanNames.put("ETC", "이더리움클래식");
        koreanNames.put("NEAR", "니어프로토콜");
        koreanNames.put("SHIB", "시바이누");
        koreanNames.put("SAND", "샌드박스");
        koreanNames.put("APE", "에이프코인");
        koreanNames.put("FIL", "파일코인");
        koreanNames.put("LTC", "라이트코인");
        koreanNames.put("BCH", "비트코인캐시");
        
        return koreanNames.getOrDefault(symbol, symbol);
    }
    
    /**
     * 심볼에 해당하는 영어 이름 반환
     */
    private String getEnglishName(String symbol) {
        Map<String, String> englishNames = new HashMap<>();
        englishNames.put("BTC", "Bitcoin");
        englishNames.put("ETH", "Ethereum");
        englishNames.put("XRP", "Ripple");
        englishNames.put("ADA", "Cardano");
        englishNames.put("DOGE", "Dogecoin");
        englishNames.put("SOL", "Solana");
        englishNames.put("DOT", "Polkadot");
        englishNames.put("AVAX", "Avalanche");
        englishNames.put("MATIC", "Polygon");
        englishNames.put("LINK", "Chainlink");
        englishNames.put("UNI", "Uniswap");
        englishNames.put("ATOM", "Cosmos");
        englishNames.put("AAVE", "Aave");
        englishNames.put("ALGO", "Algorand");
        englishNames.put("XLM", "Stellar Lumens");
        englishNames.put("ETC", "Ethereum Classic");
        englishNames.put("NEAR", "NEAR Protocol");
        englishNames.put("SHIB", "Shiba Inu");
        englishNames.put("SAND", "The Sandbox");
        englishNames.put("APE", "ApeCoin");
        englishNames.put("FIL", "Filecoin");
        englishNames.put("LTC", "Litecoin");
        englishNames.put("BCH", "Bitcoin Cash");
        
        return englishNames.getOrDefault(symbol, symbol);
    }
}