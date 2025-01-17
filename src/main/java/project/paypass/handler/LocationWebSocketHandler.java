package project.paypass.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import project.paypass.domain.dto.UserLocationDto;
import project.paypass.service.LocationWebSocketService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private final LocationWebSocketService locationWebSocketService;

    // ObjectMapper는 JSON을 객체로 변환하는 데 사용
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException { // 이거 그냥 throw 처리해도 되나?

        String payload = message.getPayload();

        // JSON을 LocationData 객체로 변환
        UserLocationDto locationData = objectMapper.readValue(payload, UserLocationDto.class);

        // 받은 위치 데이터를 출력
        System.out.println("Received location data: " + locationData);
        locationWebSocketService.saveUserLocation(locationData);

    }
}