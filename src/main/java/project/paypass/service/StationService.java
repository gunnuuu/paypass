package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.DetailLog;
import project.paypass.domain.Station;
import project.paypass.domain.dto.DetailLogDto;
import project.paypass.domain.dto.StationDto;
import project.paypass.repository.StationRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    @Transactional
    public List<Station> findAll(){
        return stationRepository.findAll();
    }

    @Transactional
    public String findBusInfoByStationNumber(Long stationNumber){
        return stationRepository.findBusInfoByStationNumber(stationNumber);
    }

    @Transactional
    public List<StationDto> findLocationsByStationNumbers(List<Long> stationNumbers) {
        // stationNumbers를 사용하여 Station 조회
        List<Station> stations = stationRepository.findByStationNumberIn(stationNumbers);

        // StationDto 리스트로 변환하여 반환
        return stations.stream()
                .map(station -> new StationDto(
                        station.getName(),
                        station.getStationNumber(),
                        station.getLatitude(),
                        station.getLongitude()
                ))
                .collect(Collectors.toList());
    }
}