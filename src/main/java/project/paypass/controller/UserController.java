package project.paypass.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.paypass.service.UserService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 첫 로그인 체크
    @PostMapping("/check-first-login")
    public ResponseEntity<Map<String, Boolean>> checkFirstLogin(@RequestBody Map<String, String> request) {
        System.out.println("첫 로그인 여부 확인 요청 도달");
        String email = request.get("email");
        System.out.println("받은 이메일: " + email);


        // 로그인 확인 로직
        boolean isFirstLogin = userService.isFirstLogin(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFirstLogin", isFirstLogin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-additional-info")
    public ResponseEntity<String> saveAdditionalInfo(@RequestBody Map<String, String> request) {
        System.out.println("받은 요청 본문: " + request);  // 전체 요청 출력

        String email = request.get("email");
        String name = request.get("name");
        String birthdate = request.get("birthdate");
        String phone = request.get("phone");

        // 추가 정보 저장 로직
        userService.saveAdditionalInfo(email, name, birthdate, phone);
        return ResponseEntity.ok("추가 정보가 저장되었습니다.");
    }

}
