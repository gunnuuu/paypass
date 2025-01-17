package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class UserLocationDto {
    private String mainId;
    private double latitude;
    private double longitude;

    public UserLocationDto(String mainId, double latitude, double longitude) {
        this.mainId = mainId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}