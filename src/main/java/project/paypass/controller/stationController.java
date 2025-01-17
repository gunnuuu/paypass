package project.paypass.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.dto.StationDto;
import project.paypass.repository.StationRepository;

import java.util.List;

@RestController
public class StationController {

    private final StationRepository stationRepository;

    public StationController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping("/stations") //http통신에서 ip주소/stations 여기통신해서 플러터에 전송하는 내용들
    public ResponseEntity<List<StationDto>> getStations() {
        List<StationDto> stationDTOs = stationRepository.findAll().stream()
                .map(station -> new StationDto( //테이블 내용중에서 id랑 역 이름빼고 번호 위치만 보내게 일단 했음 그것만 필요한거같아서
                        station.getStationNumber(),
                        station.getLatitude(),
                        station.getLongitude()
                ))
                .toList();

        return ResponseEntity.ok(stationDTOs);
    }

}