package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;
import project.paypass.repository.GeofenceLocationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeofenceService {

    private final GeofenceLocationRepository geofenceLocationRepository;
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
    public List<GeofenceLocation> startAlgorithm(String mainId){

        // mainId로 geofenceLocation 조회
        List<GeofenceLocation> geofenceLocations = geofenceLocationRepository.findByMainId(mainId);

        List<GeofenceLocation> basicGeofenceLocationList = basicAlgorithmService.algorithmStart(geofenceLocations);

        List<GeofenceLocation> averageTimeGeofenceLocationList = averageTimeAlgorithmService.algorithmStart(basicGeofenceLocationList);

        List<GeofenceLocation> duplicationDeleteGeofenceLocationList = duplicateDeleteAlgorithm.algorithmStart(averageTimeGeofenceLocationList);

        return duplicationDeleteGeofenceLocationList;
    }


}