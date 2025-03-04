package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.paypass.domain.dto.DetailLogDto;
import project.paypass.service.LogService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DetailLogController {

    private final LogService logService;

    // POST 요청을 통해 mainId와 logId를 받아 해당 detailLog들을 반환하는 API
    @PostMapping("/getDetailLogs")
    public ResponseEntity<List<DetailLogDto>> getDetailLogs(@RequestBody Map<String, Object> request) {
        String mainId = (String) request.get("mainId");  // Flutter에서 보낸 mainId 값을 가져옴
        Long logId = Long.valueOf(request.get("logId").toString());  // Flutter에서 보낸 logId 값을 가져옴

        System.out.println("mainId: " + mainId);  // mainId 출력
        System.out.println("logId: " + logId);
        // logId에 해당하는 상세 로그를 DB에서 조회
        List<DetailLogDto> detailLogs = logService.findByMain_IdandLog_Id(mainId,logId);


        return ResponseEntity.ok(detailLogs);  // 200 OK 상태와 함께 상세 로그 리스트 반환
    }
}
