package project.paypass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import project.paypass.handler.LocationWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LocationWebSocketConfig implements WebSocketConfigurer {

    private final LocationWebSocketHandler locationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(locationWebSocketHandler,"/location") // ws://주소:포트/location으로 요청이 들어오면 websocket 통신을 진행
                .setAllowedOrigins("*"); // 필요 시 CORS 설정
    }
}