package com.crypto.analysis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.crypto.analysis.service.RealTimeDataService;

@Controller
public class WebSocketController {
    
    @Autowired
    private RealTimeDataService realTimeDataService;
    
    @MessageMapping("/subscribe")
    @SendTo("/topic/ticker")
    public String subscribeToMarkets(Map<String, List<String>> subscription) {
        try {
            List<String> markets = subscription.get("markets");
            realTimeDataService.subscribeToTickerData(markets);
            return "{\"status\":\"subscribed\",\"markets\":" + markets + "}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}