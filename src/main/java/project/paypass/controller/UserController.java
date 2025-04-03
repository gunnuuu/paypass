package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.User;
import project.paypass.domain.dto.UserInfoDto;
import project.paypass.service.UserService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("mypage/info")
    public ResponseEntity<UserInfoDto> getMyPageInformation(@RequestBody Map<String, String> request){
        String userEmail = request.get("email");
        User user = userService.findByMainId(userEmail);
        UserInfoDto userInfoDto = new UserInfoDto(
                user.getMainId(),
                user.getName(),
                birthToString(user.getBirth()),
                user.getPhoneNumber());

        return ResponseEntity.ok(userInfoDto);
    }

    private String birthToString(LocalDateTime birth){
        String stringBirth = birth.toString();
        return stringBirth.substring(0,stringBirth.indexOf("T"));
    }
}