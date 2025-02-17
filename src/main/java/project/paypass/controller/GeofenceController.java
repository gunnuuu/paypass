package project.paypass.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.GeofenceLocation;
import project.paypass.domain.dto.UserGeofenceDto;
import project.paypass.service.GeofenceService;
import project.paypass.service.StationService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GeofenceController {

    private final StationService stationService;
    private final GeofenceService geofenceService;

    @PostMapping("/userFenceIn")
    @Transactional
    public ResponseEntity<Void> userGeofenceIn(@RequestBody UserGeofenceDto userGeofenceDto){
        log.info("사용자가 geofence에 접근했기 때문에 userGeofenceIn method를 실행합니다.");

        // mainId랑 stationNumber 받기
        String mainId = userGeofenceDto.getMainId();
        Long stationNumber = userGeofenceDto.getStationNumber();

        // stationNumber 활용해서 busInfo 가져오기
        String busInfo = stationService.findBusInfoByStationNumber(stationNumber);

        // geofenceLocation entity 생성
        GeofenceLocation geofenceLocation = geofenceService.createGeofenceLocation(mainId, stationNumber, busInfo);
        geofenceService.save(geofenceLocation);
        log.info("geofenceLocation 데이터를 생성 후 저장했습니다.");
        log.info("geofenceLocation: {}", geofenceLocation);

        // 메인 알고리즘 실행
        geofenceService.startAlgorithm(mainId);

        return ResponseEntity.ok().build();
    }


    @PostMapping("userFenceOut")
    public ResponseEntity<Void> userGeofenceOut(@RequestBody UserGeofenceDto userGeofenceDto){
        log.info("사용자가 geofence에서 이탈했기 때문에 userGeofenceOut method를 실행합니다.");

        // mainId랑 stationNumber 받기
        String mainId = userGeofenceDto.getMainId();
        Long stationNumber = userGeofenceDto.getStationNumber();

        // mainId와 stationNumber를 활용해서 해당 entity 가져오기
        List<GeofenceLocation> userOutStations = geofenceService.findByMainIdAndStationNumber(mainId, stationNumber);

        // entity가 존재하지 않으면 해당 데이터는 삭제한다.
        if (userOutStations.isEmpty()){
            updateFenceOutTimeNoEntity();
        }

        // entity가 하나라면 해당 entity의 fenceOutTime 추가
        if (userOutStations.size() == 1){
            updateFenceOutTimeOneEntity(userOutStations);
        }

        // 조회하였을 때 두개 이상인 경우에는 userFenceOut이 null 값인 엔티티 찾기
        if (userOutStations.size() > 1){
            updateFenceOutTimeEntities(userOutStations);
        }

        return ResponseEntity.ok().build();
    }


    private void updateFenceOutTimeNoEntity(){
        log.info("stationNumber에 부합하는 entity가 존재하지 않기 때문에 메서드를 종료합니다.");
    }

    private void updateFenceOutTimeOneEntity(List<GeofenceLocation> userOutStations){
        GeofenceLocation geofenceLocation = userOutStations.get(0);
        geofenceService.userFenceOut(geofenceLocation);
        log.info("하나의 entity를 발견하여 fenceOutTime을 추가합니다.");
    }

    private void updateFenceOutTimeEntities(List<GeofenceLocation> userOutStations) {
        userOutStations.stream()
                .filter(geofenceService::fenceOutTimeIsNull)
                .findFirst()
                .ifPresent(geofenceService::userFenceOut);
        log.info("여러 개의 entity 중 첫 번째 null fenceOutTime을 업데이트했습니다.");
    }

}