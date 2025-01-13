package project.paypass.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.dto.stationDTO;
import project.paypass.repository.stationRepository;

import java.util.List;

@RestController
public class stationController {

    private final stationRepository stationRepository;

    public stationController(stationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping("/stations") //http통신에서 ip주소/stations 여기통신해서 플러터에 전송하는 내용들
    public ResponseEntity<List<stationDTO>> getStations() {
        List<stationDTO> stationDTOs = stationRepository.findAll().stream()
                .map(station -> new stationDTO( //테이블 내용중에서 id랑 역 이름빼고 번호 위치만 보내게 일단 했음 그것만 필요한거같아서
                        station.getStationNumber(),
                        station.getLatitude(),
                        station.getLongitude()
                ))
                .toList();

        return ResponseEntity.ok(stationDTOs);
    }

}