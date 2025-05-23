package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.GeofenceLocationRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeofenceService {

    private final GeofenceLocationRepository geofenceLocationRepository;
    // 알고리즘 관련
    private final BasicAlgorithmService basicAlgorithmService;
    private final AverageTimeAlgorithmService averageTimeAlgorithmService;
    private final DuplicateDeleteAlgorithm duplicateDeleteAlgorithm;

    @Transactional
    public GeofenceLocation createGeofenceLocation(String mainId, Long stationNumber, String busInfo){
        return new GeofenceLocation(mainId,stationNumber,busInfo);
    }

    @Transactional
    public void save(GeofenceLocation geofenceLocation){
        geofenceLocationRepository.save(geofenceLocation);
    }

    @Transactional
    public List<GeofenceLocation> findByMainIdAndStationNumber(String mainId, Long stationNumber){
        return geofenceLocationRepository.findByMainIdAndStationNumber(mainId, stationNumber);
    }

    @Transactional
    public void userFenceOut(GeofenceLocation geofenceLocation) {
        geofenceLocation.userFenceOut();
    }

    @Transactional
    public boolean fenceOutTimeIsNull(GeofenceLocation geofenceLocation){
        return geofenceLocation.fenceOutTimeIsNull();
    }

    @Transactional
    public Map<List<GeofenceLocation>, List<String>> startAlgorithm(String mainId){
        // mainId로 geofenceLocation 조회
        List<GeofenceLocation> geofenceLocations = geofenceLocationRepository.findByMainId(mainId);

        Map<List<GeofenceLocation>, List<String>> resultMap = mainAlgorithm(geofenceLocations);

        return resultMap;
    }

    @Transactional
    public Map<List<GeofenceLocation>, List<String>> mainAlgorithm(List<GeofenceLocation> geofenceLocations){
        // 알고리즘 실행
        Map<String, List<Long>> basicMap = basicAlgorithmService.algorithmStart(geofenceLocations);
        Map<String, List<Long>> averageTimeMap = averageTimeAlgorithmService.algorithmStart(basicMap, geofenceLocations);
        Map<List<GeofenceLocation>, List<String>> resultMap = duplicateDeleteAlgorithm.algorithmStart(averageTimeMap, geofenceLocations);

        return resultMap;
    }

    @Transactional
    public List<GeofenceLocation> findByMainId(String mainId){
        return geofenceLocationRepository.findByMainId(mainId);
    }

}