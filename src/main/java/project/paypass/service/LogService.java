package project.paypass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.DetailLog;
import project.paypass.domain.GeofenceLocation;
import project.paypass.domain.Log;
import project.paypass.repository.DetailLogRepository;
import project.paypass.repository.LogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final DetailLogRepository detailLogRepository;

    public void saveLogData(String mainId, Map<List<GeofenceLocation>, List<String>> resultMap){
        log.info("resultMap 확인: " + resultMap);

        for (var geofenceLocationListAndRouteIdList : resultMap.entrySet()) {
            List<GeofenceLocation> geofenceLocationList = geofenceLocationListAndRouteIdList.getKey();
            List<String> routeIdList = geofenceLocationListAndRouteIdList.getValue();
            // log save
            Log logData = saveLog(mainId, geofenceLocationList, routeIdList);
            // detailLog save
            saveDetailLogData(mainId, logData, geofenceLocationList);
        }
    }

    private Log saveLog(String mainId, List<GeofenceLocation> geofenceLocationList, List<String> routeIdList) {
        // 도착 정류장 정보
        GeofenceLocation arrival = geofenceLocationList.get(geofenceLocationList.size() - 1);
        LocalDateTime arrivalTime = arrival.getFenceInTime();
        Long arrivalStationNumber = arrival.getStationNumber();

        log.info("추출된 arrivalStationNumber: " + arrivalStationNumber);


        // arrivalStationNumber가 null일 경우 기본값 설정
        if (arrivalStationNumber == null) {
            arrivalStationNumber = 0L; // 기본값 설정 (예: 0)
        }

        // routeIdList String으로 변환
        String routeIdString = String.join(",", routeIdList);

        Log logData = new Log(mainId, arrivalTime, arrivalStationNumber, routeIdString);

        logRepository.save(logData);
        log.info("log data 저장했습니다." + logData);

        return logData;
    }

    private void saveDetailLogData(String mainId, Log logData, List<GeofenceLocation> geofenceLocationList) {
        for (GeofenceLocation geofenceLocation : geofenceLocationList) {
            LocalDateTime fenceInTime = geofenceLocation.getFenceInTime();
            LocalDateTime fenceOutTime = geofenceLocation.getFenceOutTime();
            Long stationNumber = geofenceLocation.getStationNumber();

            DetailLog detailLog = new DetailLog(mainId, logData, fenceInTime, fenceOutTime, stationNumber);

            detailLogRepository.save(detailLog);

            log.info("detailLog data 저장했습니다." + detailLog);
        }
    }

}