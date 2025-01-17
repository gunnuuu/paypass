package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class StationDto {
    private Long stationNumber;
    private Double latitude;
    private Double longitude;

    public StationDto(Long stationNumber, Double latitude, Double longitude) {
        this.stationNumber = stationNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}