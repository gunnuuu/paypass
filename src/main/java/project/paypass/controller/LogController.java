package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.dto.LogDto; // LogDto 임포트
import project.paypass.service.LogService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/getLogs")
    public ResponseEntity<List<LogDto>> getLogs(@RequestBody Map<String, String> request) {
        String mainId = request.get("mainId");  // Flutter에서 보낸 mainId 값을 가져옴
        List<LogDto> logs = logService.getLogsByMainId(mainId);
        return ResponseEntity.ok(logs);
    }
}
