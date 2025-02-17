package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class UserGeofenceDto {
    private String mainId;
    private Long stationNumber;

    public UserGeofenceDto(String mainId, Long stationNumber) {
        this.mainId = mainId;
        this.stationNumber = stationNumber;
    }
}