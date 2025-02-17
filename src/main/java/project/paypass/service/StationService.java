package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.Station;
import project.paypass.repository.StationRepository;

import java.util.List;

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

}