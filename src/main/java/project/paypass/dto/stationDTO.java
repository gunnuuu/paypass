package project.paypass.dto;

public class stationDTO { //DTO 만들어야 그 findALl()로 가져오고 그중 원하는 값만 사용가눙
    private String stationNumber;
    private Double latitude;
    private Double longitude;

    public stationDTO(String stationNumber, Double latitude, Double longitude) {
        this.stationNumber = stationNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getter 메서드
    public String getStationNumber() {
        return stationNumber;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}