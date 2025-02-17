package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.dto.UserInfoDto;
import project.paypass.service.LoginHttpService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginHttpService loginHttpService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> checkLogin(@RequestBody Map<String, String> request) {
        String googleId = request.get("googleId");

        if (googleId == null || googleId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean checkNewUser = loginHttpService.checkNewUser(googleId);
        Map<String, String> response = new HashMap<>();

        if (checkNewUser) {
            response.put("status", "NEW_USER"); 
        }

        if (!checkNewUser) {
            response.put("status", "EXISTING_USER");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/new-user")
    public ResponseEntity<Void> registerNewUser(@RequestBody UserInfoDto userInfoDto){
        loginHttpService.saveNewUser(userInfoDto);
        return ResponseEntity.ok().build();
    }

}