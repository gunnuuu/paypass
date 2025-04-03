package project.paypass.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.GeofenceLocation;
import project.paypass.service.GeofenceService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class adminController {

    private final GeofenceService geofenceService;

    @PostMapping("/test/algorithmStart")
    public ResponseEntity<Map<List<GeofenceLocation>, List<String>>> algorithmStart(@RequestBody Map<String, String> payload) { // 일단 dto를 사용하지 않고 Map으로 해결

        String mainId = payload.get("mainId");
        log.info("mainId = "+ mainId +", 메인 알고리즘 테스트를 실행합니다.");

        Map<List<GeofenceLocation>, List<String>> resultMap = geofenceService.startAlgorithm(mainId);

        for (var geofenceLocationListAndRouteIdList : resultMap.entrySet()) {
            List<GeofenceLocation> geofenceLocationList = geofenceLocationListAndRouteIdList.getKey();
            List<String> routeIdList = geofenceLocationListAndRouteIdList.getValue();
            log.info("geofenceLocation 리스트 = " + geofenceLocationList);
            log.info("routeId 리스트 = " + routeIdList);
        }

        return ResponseEntity.ok(resultMap);
    }

    @PostMapping("/test/geofenceLocations")
    public ResponseEntity<List<GeofenceLocation>> getGeofenceLocations(@RequestBody Map<String, String> payload) { // 일단 dto를 사용하지 않고 Map으로 해결

        String mainId = payload.get("mainId");
        log.info("mainId = "+ mainId +", 메인 알고리즘 테스트를 실행합니다.");

        List<GeofenceLocation> geofenceLocations = geofenceService.findByMainId(mainId);

        return ResponseEntity.ok(geofenceLocations);
    }
}