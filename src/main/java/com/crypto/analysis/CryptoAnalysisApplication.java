package com.crypto.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.crypto.analysis.service.RealTimeDataService;  // javax 대신 jakarta 사용

import jakarta.annotation.PreDestroy;

@SpringBootApplication(scanBasePackages = "com.crypto.analysis")
@EnableScheduling
@EnableWebSocketMessageBroker
public class CryptoAnalysisApplication implements WebSocketMessageBrokerConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(CryptoAnalysisApplication.class, args);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 이 부분을 수정
        registry.addEndpoint("/ws")
                // "*" 대신 구체적인 출처 지정
                .setAllowedOrigins("http://localhost:8080") 
                // 또는 아래와 같이 credentials 없이 설정
                // .setAllowedOrigins("*").setAllowCredentials(false)
                .withSockJS();
    }

    @Autowired
    private RealTimeDataService realTimeDataService;

    @PreDestroy
    public void onShutdown() {
        System.out.println("애플리케이션 종료, 리소스 정리 중...");
        if (realTimeDataService != null) {
            realTimeDataService.shutdown();
        }
    }
}