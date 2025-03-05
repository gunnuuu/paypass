package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class StationDto {
    private String name;
    private Long stationNumber;
    private double latitude;
    private double longitude;

    public StationDto(String name, Long stationNumber, double latitude, double longitude) {
        this.name = name;
        this.stationNumber = stationNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public StationDto(double latitude, double longitude, Long stationNumber) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.stationNumber = stationNumber;
    }
}