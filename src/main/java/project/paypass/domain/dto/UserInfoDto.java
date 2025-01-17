package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class UserInfoDto {

    private String mainId;
    private String name;
    private String birth;
    private String phoneNumber;

    public UserInfoDto(String mainId, String name, String birth, String phoneNumber) {
        this.mainId = mainId;
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
    }
}