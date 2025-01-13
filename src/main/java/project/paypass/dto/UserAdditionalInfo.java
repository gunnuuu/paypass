package project.paypass.dto;

// DTO 클래스
import lombok.Data;

@Data
public class UserAdditionalInfo {
    private String email;
    private String name;
    private String birthdate;
    private String phone;
}
