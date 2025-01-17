package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.User;
import project.paypass.domain.Wallet;
import project.paypass.domain.dto.UserInfoDto;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginHttpService {
    private final UserService userService;
    private final WalletService walletService;

    @Transactional
    public boolean checkNewUser(String mainId){
        return userService.checkNewUser(mainId);
    }

    @Transactional
    public void saveNewUser(UserInfoDto userInfoDto){
        String mainId = userInfoDto.getMainId();
        String name = userInfoDto.getName();
        String phoneNumber = userInfoDto.getPhoneNumber();

        // birth 데이터 변형
        String birthString = userInfoDto.getBirth();
        LocalDateTime birth = LocalDateTime.parse(birthString+"T00:00:00");

        // 이후 회원가입 기능이 추가된다면 new Login() 필요함
        userService.save(new User(mainId,name,birth,phoneNumber));
        walletService.save(new Wallet(mainId));
    }

}