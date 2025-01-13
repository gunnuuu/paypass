package project.paypass.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.paypass.domain.user;
import project.paypass.repository.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 첫 로그인 여부를 확인하는 메서드
    public boolean isFirstLogin(String email) {
        System.out.println("Email 존재 여부 확인: " + userRepository.existsBymainid(email));
        return !userRepository.existsBymainid(email); // 이메일로 사용자 조회, 없으면 첫 로그인
    }

    public void saveAdditionalInfo( String email, String name, String birthdate, String phone) {
        user user = new user();

        // 이메일로 mainid설정
        user.setMainId(email);
        // 이름, 생년월일, 전화번호 설정
        user.setName(name);

        // 날짜 형식 처리
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate birthDate = LocalDate.parse(birthdate.trim(), formatter);
        user.setBirth(birthDate);

        user.setPhoneNumber(phone);

        // 저장할 때 mainid가 제대로 설정되어 있는지 확인
        System.out.println("저장할 사용자 정보: " + user);

        // 사용자를 저장 (기존 데이터가 있으면 업데이트)
        userRepository.save(user);
    }

}
