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
        try {
            String[] codes = markets.toArray(new String[0]);
            
            client.doHandshake(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    String message = "{\"type\":\"ticker\",\"codes\":" + objectMapper.writeValueAsString(codes) + "}";
                    session.sendMessage(new TextMessage(message));
                    System.out.println("업비트 WebSocket 연결 성공, 구독 요청 전송: " + message);
                }
                
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    String payload = message.getPayload();
                    // 받은 데이터를 클라이언트에게 전달
                    messagingTemplate.convertAndSend("/topic/ticker", payload);
                }
                
                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                    System.err.println("업비트 WebSocket 전송 오류: " + exception.getMessage());
                    super.handleTransportError(session, exception);
                }
                
                @Override
                public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
                    System.out.println("업비트 WebSocket 연결 종료: " + status);
                    // 10초 후 재연결 시도
                    Thread.sleep(10000);
                    subscribeToTickerData(markets);
                }
            }, "wss://api.upbit.com/websocket/v1").get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("업비트 WebSocket 연결 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 10초 후 재시도
            try {
                Thread.sleep(10000);
                subscribeToTickerData(markets);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}