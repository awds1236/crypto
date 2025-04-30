package com.crypto.analysis.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RealTimeDataService {
    
    private final WebSocketClient client;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public RealTimeDataService() {
        this.client = new StandardWebSocketClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public void subscribeToTickerData(List<String> markets) {
        String[] codes = markets.toArray(new String[0]);
        
        try {
            client.doHandshake(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    String message = "{\"type\":\"ticker\",\"codes\":" + objectMapper.writeValueAsString(codes) + "}";
                    session.sendMessage(new TextMessage(message));
                }
                
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    String payload = message.getPayload();
                    // 받은 데이터를 클라이언트에게 전달
                    messagingTemplate.convertAndSend("/topic/ticker", payload);
                }
            }, "wss://api.upbit.com/websocket/v1").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
