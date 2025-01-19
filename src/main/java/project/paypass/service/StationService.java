package project.paypass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.Station;
import project.paypass.repository.StationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    public List<Station> findAll(){
        return stationRepository.findAll();
    }

}