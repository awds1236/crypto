package com.crypto.analysis.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.crypto.analysis.model.Stock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class StockService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${alphavantage.api.key}")
    private String ALPHA_VANTAGE_API_KEY;
    
    @Value("${newsapi.api.key}")
    private String NEWS_API_KEY;
    
    @Value("${finnhub.api.key:}")
    private String FINNHUB_API_KEY;
    
    public StockService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 상위 거래량 종목 가져오기
     */
    @Cacheable(value = "topVolumeStocks", key = "#count")
    public List<Stock> getTopVolumeStocks(int count) {
        try {
            // Alpha Vantage API는 상위 거래량 API가 없어서 Finnhub API 사용
            if (!FINNHUB_API_KEY.isEmpty()) {
                String url = "https://finnhub.io/api/v1/stock/symbol?exchange=US&token=" + FINNHUB_API_KEY;
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                JsonNode data = objectMapper.readTree(response.getBody());
                
                List<Stock> allStocks = new ArrayList<>();
                for (JsonNode stockNode : data) {
                    Stock stock = new Stock();
                    stock.setSymbol(stockNode.get("symbol").asText());
                    stock.setName(stockNode.get("description").asText());
                    stock.setExchange("US");
                    
                    // 각 종목의 상세 정보 가져오기 (거래량 등)
                    try {
                        Map<String, Object> quoteData = getStockData(stock.getSymbol(), "US");
                        stock.setPrice((double) quoteData.getOrDefault("currentPrice", 0.0));
                        stock.setVolume((long) quoteData.getOrDefault("volume", 0L));
                        stock.setChangePercent((double) quoteData.getOrDefault("changePercent", 0.0));
                        allStocks.add(stock);
                    } catch (Exception e) {
                        // 개별 종목 에러 무시, 계속 진행
                        continue;
                    }
                }
                
                // 거래량 기준 정렬 후 상위 N개 반환
                return allStocks.stream()
                    .sorted((s1, s2) -> Long.compare(s2.getVolume(), s1.getVolume()))
                    .limit(count)
                    .collect(Collectors.toList());
            } else {
                // Finnhub API Key가 없으면 기본 종목 반환
                return getDefaultStocks();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultStocks();
        }
    }
    
    /**
     * 종목 검색
     */
    public List<Stock> searchStocks(String query, String market) {
        try {
            String url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + query
                + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode data = objectMapper.readTree(response.getBody());
            
            List<Stock> searchResults = new ArrayList<>();
            if (data.has("bestMatches")) {
                JsonNode matches = data.get("bestMatches");
                
                for (JsonNode match : matches) {
                    String matchedMarket = match.get("4. region").asText();
                    // 선택한 시장과 일치하는 종목만 필터링 (대략적으로)
                    if (market.equals("US") && (matchedMarket.equals("United States") || matchedMarket.equals("USA"))) {
                        Stock stock = new Stock();
                        stock.setSymbol(match.get("1. symbol").asText());
                        stock.setName(match.get("2. name").asText());
                        stock.setExchange(match.get("3. type").asText() + " " + match.get("4. region").asText());
                        searchResults.add(stock);
                    }
                }
            }
            
            return searchResults;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 종목 시세 정보 가져오기
     */
    public Map<String, Object> getStockData(String symbol, String market) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Global Quote API 사용
            String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol
                + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode data = objectMapper.readTree(response.getBody());
            
            if (data.has("Global Quote") && !data.get("Global Quote").isEmpty()) {
                JsonNode quote = data.get("Global Quote");
                
                double price = Double.parseDouble(quote.get("05. price").asText());
                double previousClose = Double.parseDouble(quote.get("08. previous close").asText());
                double change = Double.parseDouble(quote.get("09. change").asText());
                double changePercent = Double.parseDouble(quote.get("10. change percent").asText().replace("%", "")) / 100.0;
                long volume = Long.parseLong(quote.get("06. volume").asText());
                
                result.put("symbol", symbol);
                result.put("currentPrice", price);
                result.put("previousClose", previousClose);
                result.put("change", change);
                result.put("changePercent", changePercent);
                result.put("volume", volume);
                
                // 회사 정보 추가
                Map<String, Object> companyInfo = getCompanyOverview(symbol);
                result.put("companyName", companyInfo.getOrDefault("Name", symbol));
                result.put("marketCap", companyInfo.getOrDefault("MarketCapitalization", "N/A"));
                result.put("pe", companyInfo.getOrDefault("PERatio", "N/A"));
                result.put("eps", companyInfo.getOrDefault("EPS", "N/A"));
                result.put("dividend", companyInfo.getOrDefault("DividendYield", "N/A"));
                result.put("sector", companyInfo.getOrDefault("Sector", "N/A"));
                result.put("industry", companyInfo.getOrDefault("Industry", "N/A"));
                
                return result;
            } else {
                // API 제한 또는 심볼이 없는 경우 기본값 반환
                throw new Exception("시세 정보를 가져올 수 없습니다.");
            }
        } catch (Exception e) {
            // 기본 데이터 반환
            result.put("symbol", symbol);
            result.put("companyName", getDefaultCompanyName(symbol));
            result.put("currentPrice", getDefaultPrice(symbol));
            result.put("previousClose", getDefaultPrice(symbol) * 0.99);
            result.put("change", getDefaultPrice(symbol) * 0.01);
            result.put("changePercent", 0.01);
            result.put("volume", 1000000L);
            
            return result;
        }
    }
    
    /**
     * 회사 개요 정보 가져오기
     */
    @Cacheable(value = "companyOverview", key = "#symbol")
    public Map<String, Object> getCompanyOverview(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=OVERVIEW&symbol=" + symbol
                + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode data = objectMapper.readTree(response.getBody());
            
            Map<String, Object> overview = new HashMap<>();
            data.fields().forEachRemaining(field -> {
                overview.put(field.getKey(), field.getValue().asText());
            });
            
            return overview;
        } catch (Exception e) {
            // 기본 데이터 반환
            Map<String, Object> defaultOverview = new HashMap<>();
            defaultOverview.put("Name", getDefaultCompanyName(symbol));
            defaultOverview.put("MarketCapitalization", "10000000000");
            defaultOverview.put("PERatio", "20");
            defaultOverview.put("EPS", "2.5");
            defaultOverview.put("DividendYield", "1.5");
            defaultOverview.put("Sector", "Technology");
            defaultOverview.put("Industry", "Software");
            defaultOverview.put("Description", symbol + "은 글로벌 기술 기업입니다.");
            
            return defaultOverview;
        }
    }
    
    /**
     * 과거 데이터 가져오기
     */
    public String getHistoricalData(String symbol, String market, int days) {
        try {
            // Daily API 사용
            String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + symbol
                + "&outputsize=compact&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode data = objectMapper.readTree(response.getBody());
            
            if (data.has("Time Series (Daily)")) {
                JsonNode timeSeries = data.get("Time Series (Daily)");
                
                ArrayNode resultArray = objectMapper.createArrayNode();
                
                // 날짜 기준 정렬 (최신 -> 과거)
                List<String> dates = new ArrayList<>();
                timeSeries.fieldNames().forEachRemaining(dates::add);
                dates.sort((d1, d2) -> d2.compareTo(d1));
                
                // 지정된 일수만큼만 데이터 추출
                for (int i = 0; i < Math.min(days, dates.size()); i++) {
                    String date = dates.get(i);
                    JsonNode dayData = timeSeries.get(date);
                    
                    // 업비트 형식과 유사하게 변환
                    resultArray.add(objectMapper.createObjectNode()
                        .put("market", symbol)
                        .put("candle_date_time_utc", date + "T00:00:00")
                        .put("candle_date_time_kst", date + "T09:00:00")
                        .put("opening_price", Double.parseDouble(dayData.get("1. open").asText()))
                        .put("high_price", Double.parseDouble(dayData.get("2. high").asText()))
                        .put("low_price", Double.parseDouble(dayData.get("3. low").asText()))
                        .put("trade_price", Double.parseDouble(dayData.get("4. close").asText()))
                        .put("candle_acc_trade_price", Double.parseDouble(dayData.get("5. volume").asText()) * 
                                                      Double.parseDouble(dayData.get("4. close").asText()))
                        .put("candle_acc_trade_volume", Double.parseDouble(dayData.get("5. volume").asText())));
                }
                
                return objectMapper.writeValueAsString(resultArray);
            } else {
                // API 제한 또는 심볼이 없는 경우
                throw new Exception("과거 데이터를 가져올 수 없습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            
            // 기본 데이터 생성
            return generateDefaultHistoricalData(symbol, days);
        }
    }
    
    /**
     * 뉴스 데이터 가져오기
     */
    @Cacheable(value = "stockNews", key = "#symbol", unless = "#result == null")
    public Map<String, Object> getNewsForStock(String symbol) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> articles = new ArrayList<>();
        
        try {
            // Alpha Vantage News API 사용
            String url = "https://www.alphavantage.co/query?function=NEWS_SENTIMENT&tickers=" + symbol
                + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode data = objectMapper.readTree(response.getBody());
            
            if (data.has("feed")) {
                JsonNode feed = data.get("feed");
                
                int count = 0;
                for (JsonNode article : feed) {
                    if (count >= 10) break;  // 최대 10개만 가져오기
                    
                    Map<String, Object> articleMap = new HashMap<>();
                    articleMap.put("title", article.get("title").asText());
                    articleMap.put("summary", article.has("summary") ? article.get("summary").asText() : "");
                    articleMap.put("url", article.get("url").asText());
                    articleMap.put("publishedAt", article.get("time_published").asText());
                    articleMap.put("source", article.get("source").asText());
                    
                    // 감성 점수 (있는 경우)
                    if (article.has("overall_sentiment_score")) {
                        articleMap.put("sentimentScore", article.get("overall_sentiment_score").asDouble());
                        articleMap.put("sentimentLabel", article.get("overall_sentiment_label").asText());
                    }
                    
                    articles.add(articleMap);
                    count++;
                }
            }
            
            // NewsAPI 사용 (추가 뉴스)
            if (articles.size() < 5 && !NEWS_API_KEY.isEmpty()) {
                String newsApiUrl = "https://newsapi.org/v2/everything?q=" + symbol +
                    "&language=en&sortBy=publishedAt&pageSize=5&apiKey=" + NEWS_API_KEY;
                
                ResponseEntity<String> newsResponse = restTemplate.getForEntity(newsApiUrl, String.class);
                JsonNode newsData = objectMapper.readTree(newsResponse.getBody());
                
                if (newsData.has("articles")) {
                    JsonNode newsArticles = newsData.get("articles");
                    
                    for (JsonNode article : newsArticles) {
                        if (articles.size() >= 10) break;  // 최대 10개
                        
                        Map<String, Object> articleMap = new HashMap<>();
                        articleMap.put("title", article.get("title").asText());
                        articleMap.put("summary", article.has("description") ? article.get("description").asText() : "");
                        articleMap.put("url", article.get("url").asText());
                        articleMap.put("publishedAt", article.get("publishedAt").asText());
                        articleMap.put("source", article.get("source").get("name").asText());
                        
                        articles.add(articleMap);
                    }
                }
            }
            
            result.put("symbol", symbol);
            result.put("articlesCount", articles.size());
            result.put("articles", articles);
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            
            // 기본 뉴스 생성
            result.put("symbol", symbol);
            result.put("articlesCount", 0);
            result.put("articles", new ArrayList<>());
            
            return result;
        }
    }
    
    /**
     * 재무 데이터 가져오기
     */
    @Cacheable(value = "financialData", key = "#symbol + '-' + #market", unless = "#result == null")
    public Map<String, Object> getFinancialData(String symbol, String market) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 회사 개요 (이미 캐시된 경우 사용)
            Map<String, Object> overview = getCompanyOverview(symbol);
            
            // 필요한 재무 데이터 추출 또는 변환
            double marketCap = 0.0;
            if (overview.containsKey("MarketCapitalization")) {
                marketCap = Double.parseDouble(overview.getOrDefault("MarketCapitalization", "0").toString());
            }
            
            double pe = 0.0;
            if (overview.containsKey("PERatio") && !overview.get("PERatio").equals("None")) {
                pe = Double.parseDouble(overview.getOrDefault("PERatio", "0").toString());
            }
            
            double eps = 0.0;
            if (overview.containsKey("EPS") && !overview.get("EPS").equals("None")) {
                eps = Double.parseDouble(overview.getOrDefault("EPS", "0").toString());
            }
            
            double dividend = 0.0;
            if (overview.containsKey("DividendYield") && !overview.get("DividendYield").equals("None")) {
                String dividendStr = overview.getOrDefault("DividendYield", "0").toString();
                dividend = Double.parseDouble(dividendStr) * 100; // 백분율로 변환
            }
            
            double beta = 0.0;
            if (overview.containsKey("Beta") && !overview.get("Beta").equals("None")) {
                beta = Double.parseDouble(overview.getOrDefault("Beta", "0").toString());
            }
            
            // ROE = 당기순이익 / 자기자본 (대략적으로 계산)
            double roe = 0.0;
            if (overview.containsKey("ReturnOnEquityTTM") && !overview.get("ReturnOnEquityTTM").equals("None")) {
                roe = Double.parseDouble(overview.getOrDefault("ReturnOnEquityTTM", "0").toString()) * 100; // 백분율로 변환
            }
            
            // 결과 저장
            result.put("marketCap", marketCap);
            result.put("pe", pe);
            result.put("eps", eps);
            result.put("dividend", dividend);
            result.put("beta", beta);
            result.put("roe", roe);
            result.put("sector", overview.getOrDefault("Sector", ""));
            result.put("industry", overview.getOrDefault("Industry", ""));
            result.put("description", overview.getOrDefault("Description", ""));
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            
            // 기본 재무 데이터
            result.put("marketCap", 50000000000.0);  // 500억 달러
            result.put("pe", 20.0);                 // P/E 비율
            result.put("eps", 5.0);                 // EPS
            result.put("dividend", 1.5);            // 배당 수익률 (%)
            result.put("beta", 1.2);                // 베타
            result.put("roe", 15.0);                // ROE (%)
            result.put("sector", "Technology");     // 업종
            result.put("industry", "Software");     // 산업
            result.put("description", symbol + "은 글로벌 기술 기업입니다.");
            
            return result;
        }
    }
    
    /**
     * 기본 종목 리스트 반환 (API 장애 시)
     */
    public List<Stock> getDefaultStocks() {
        List<Stock> defaultStocks = new ArrayList<>();
        
        // 주요 미국 주식
        String[][] stockData = {
            {"AAPL", "Apple Inc.", "NASDAQ", "180.25", "3500000000"},
            {"MSFT", "Microsoft Corporation", "NASDAQ", "330.15", "2800000000"},
            {"GOOGL", "Alphabet Inc.", "NASDAQ", "140.35", "2200000000"},
            {"AMZN", "Amazon.com Inc.", "NASDAQ", "132.50", "2100000000"},
            {"TSLA", "Tesla Inc.", "NASDAQ", "240.75", "1900000000"},
            {"META", "Meta Platforms Inc.", "NASDAQ", "310.20", "1700000000"},
            {"NVDA", "NVIDIA Corporation", "NASDAQ", "430.45", "1600000000"},
            {"BRK-B", "Berkshire Hathaway Inc.", "NYSE", "350.10", "1400000000"},
            {"JPM", "JPMorgan Chase & Co.", "NYSE", "150.70", "1300000000"},
            {"V", "Visa Inc.", "NYSE", "240.30", "1200000000"},
            {"PG", "Procter & Gamble Co.", "NYSE", "155.90", "1100000000"},
            {"UNH", "UnitedHealth Group Inc.", "NYSE", "510.25", "1000000000"},
            {"JNJ", "Johnson & Johnson", "NYSE", "165.45", "950000000"},
            {"MA", "Mastercard Inc.", "NYSE", "375.60", "900000000"},
            {"HD", "Home Depot Inc.", "NYSE", "340.20", "850000000"},
            {"BAC", "Bank of America Corp.", "NYSE", "35.75", "800000000"},
            {"XOM", "Exxon Mobil Corporation", "NYSE", "110.50", "750000000"},
            {"PFE", "Pfizer Inc.", "NYSE", "32.40", "700000000"},
            {"CSCO", "Cisco Systems Inc.", "NASDAQ", "55.30", "650000000"},
            {"DIS", "Walt Disney Co.", "NYSE", "98.65", "600000000"},
            {"VZ", "Verizon Communications Inc.", "NYSE", "42.80", "550000000"},
            {"NFLX", "Netflix Inc.", "NASDAQ", "420.15", "500000000"},
            {"KO", "Coca-Cola Co.", "NYSE", "62.35", "450000000"},
            {"WMT", "Walmart Inc.", "NYSE", "175.50", "400000000"},
            {"CRM", "Salesforce Inc.", "NYSE", "210.70", "350000000"},
            {"MRK", "Merck & Co. Inc.", "NYSE", "115.90", "300000000"},
            {"AMD", "Advanced Micro Devices Inc.", "NASDAQ", "110.25", "250000000"},
            {"INTC", "Intel Corporation", "NASDAQ", "38.15", "200000000"},
            {"T", "AT&T Inc.", "NYSE", "16.90", "150000000"},
            {"NKE", "Nike Inc.", "NYSE", "120.30", "100000000"}
        };
        
        for (String[] data : stockData) {
            Stock stock = new Stock();
            stock.setSymbol(data[0]);
            stock.setName(data[1]);
            stock.setExchange(data[2]);
            stock.setPrice(Double.parseDouble(data[3]));
            stock.setVolume(Long.parseLong(data[4]));
            stock.setChangePercent(Math.random() * 4 - 2); // -2% ~ 2% 변동
            stock.setChange(stock.getPrice() * stock.getChangePercent() / 100);
            defaultStocks.add(stock);
        }
        
        return defaultStocks;
    }
    
    /**
     * 기본 과거 데이터 생성 (API 장애 시)
     */
    private String generateDefaultHistoricalData(String symbol, int days) {
        try {
            ArrayNode candles = objectMapper.createArrayNode();
            double basePrice = getDefaultPrice(symbol);
            
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(formatter);
                
                // 랜덤 가격 변동 (-1.5% ~ +1.5%)
                double fluctuation = (Math.random() * 3 - 1.5) / 100;
                double openPrice = basePrice * (1 + fluctuation);
                double closePrice = openPrice * (1 + (Math.random() * 2 - 1) / 100);
                double highPrice = Math.max(openPrice, closePrice) * (1 + Math.random() / 100);
                double lowPrice = Math.min(openPrice, closePrice) * (1 - Math.random() / 100);
                double volume = 1000000 + Math.random() * 2000000;
                
                candles.add(objectMapper.createObjectNode()
                    .put("market", symbol)
                    .put("candle_date_time_utc", dateStr + "T00:00:00")
                    .put("candle_date_time_kst", dateStr + "T09:00:00")
                    .put("opening_price", openPrice)
                    .put("high_price", highPrice)
                    .put("low_price", lowPrice)
                    .put("trade_price", closePrice)
                    .put("candle_acc_trade_price", volume * closePrice)
                    .put("candle_acc_trade_volume", volume));
                
                // 다음 날의 기준 가격 업데이트
                basePrice = closePrice;
            }
            
            return objectMapper.writeValueAsString(candles);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
    
    /**
     * 기본 종목 가격 (API 장애 시)
     */
    private double getDefaultPrice(String symbol) {
        Map<String, Double> defaultPrices = new HashMap<>();
        defaultPrices.put("AAPL", 180.0);
        defaultPrices.put("MSFT", 330.0);
        defaultPrices.put("GOOGL", 140.0);
        defaultPrices.put("AMZN", 130.0);
        defaultPrices.put("TSLA", 240.0);
        defaultPrices.put("META", 310.0);
        defaultPrices.put("NVDA", 430.0);
        
        return defaultPrices.getOrDefault(symbol, 100.0);
    }
    
    /**
     * 기본 회사명 (API 장애 시)
     */
    private String getDefaultCompanyName(String symbol) {
        Map<String, String> defaultNames = new HashMap<>();
        defaultNames.put("AAPL", "Apple Inc.");
        defaultNames.put("MSFT", "Microsoft Corporation");
        defaultNames.put("GOOGL", "Alphabet Inc.");
        defaultNames.put("AMZN", "Amazon.com Inc.");
        defaultNames.put("TSLA", "Tesla Inc.");
        defaultNames.put("META", "Meta Platforms Inc.");
        defaultNames.put("NVDA", "NVIDIA Corporation");
        
        return defaultNames.getOrDefault(symbol, symbol + " Corporation");
    }
}