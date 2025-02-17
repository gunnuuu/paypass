package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import project.paypass.domain.GeofenceLocation;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
public class GeofenceLocationDto {
    private String mainId;
    private LocalDateTime fenceInTime;
    private LocalDateTime fenceOutTime;
    private Long stationNumber;
    private String busInfo;
    private boolean board;

    // ✅ 엔티티에서 DTO로 변환하는 생성자 추가
    public GeofenceLocationDto(GeofenceLocation geofenceLocation) {
        this.mainId = geofenceLocation.getMainId();
        this.fenceInTime = geofenceLocation.getFenceInTime();
        this.fenceOutTime = geofenceLocation.getFenceOutTime();
        this.stationNumber = geofenceLocation.getStationNumber();
        this.busInfo = geofenceLocation.getBusInfo();
    }
}
