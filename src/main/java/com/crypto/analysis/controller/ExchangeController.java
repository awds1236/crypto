package com.crypto.analysis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.crypto.analysis.service.BinanceService;
import com.crypto.analysis.service.UpbitService;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {
    
    @Autowired
    private UpbitService upbitService;
    
    @Autowired
    private BinanceService binanceService;
    
    /**
     * 거래소별 코인 목록 조회
     */
    @GetMapping("/markets")
    @ResponseBody
    public String getMarkets(@RequestParam String exchange) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getMarkets();
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getSymbols();
            } else {
                return "{\"error\": \"지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.\"}";
            }
        } catch (Exception e) {
            System.err.println("코인 목록 조회 실패: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 현재가 조회
     */
    @GetMapping("/price")
    @ResponseBody
    public String getCurrentPrice(@RequestParam String exchange, @RequestParam String symbol) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getCurrentPrice(symbol);
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getCurrentPrice(symbol);
            } else {
                return "{\"error\": \"지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.\"}";
            }
        } catch (Exception e) {
            System.err.println("현재가 조회 실패: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 일봉 캔들 조회
     */
    @GetMapping("/candles/day")
    @ResponseBody
    public String getDayCandles(
            @RequestParam String exchange, 
            @RequestParam String symbol, 
            @RequestParam(defaultValue = "30") int count) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getDayCandles(symbol, count);
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getDayCandles(symbol, count);
            } else {
                return "{\"error\": \"지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.\"}";
            }
        } catch (Exception e) {
            System.err.println("일봉 조회 실패: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 시간봉 캔들 조회
     */
    @GetMapping("/candles/hour")
    @ResponseBody
    public String getHourCandles(
            @RequestParam String exchange,
            @RequestParam String symbol, 
            @RequestParam(defaultValue = "24") int count) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getHourCandles(symbol, count);
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getHourCandles(symbol, count);
            } else {
                return "{\"error\": \"지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.\"}";
            }
        } catch (Exception e) {
            System.err.println("시간봉 조회 실패: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 분봉 캔들 조회
     */
    @GetMapping("/candles/minute")
    @ResponseBody
    public String getMinuteCandles(
            @RequestParam String exchange,
            @RequestParam String symbol, 
            @RequestParam(defaultValue = "1") int minutes,
            @RequestParam(defaultValue = "60") int count) {
        try {
            if ("upbit".equalsIgnoreCase(exchange)) {
                return upbitService.getMinuteCandles(symbol, minutes, count);
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return binanceService.getMinuteCandles(symbol, minutes, count);
            } else {
                return "{\"error\": \"지원하지 않는 거래소입니다. 'upbit' 또는 'binance'를 선택하세요.\"}";
            }
        } catch (Exception e) {
            System.err.println("분봉 조회 실패: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}