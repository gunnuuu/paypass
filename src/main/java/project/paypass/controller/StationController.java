package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.dto.StationDto;
import project.paypass.service.StationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping("/getStations")
    public ResponseEntity<List<StationDto>> getStations(){
        List<StationDto> stations = stationService.findAll().stream()
                .map(station -> new StationDto(
                        station.getName(),
                        station.getStationNumber(),
                        station.getLatitude(),
                        station.getLongitude()
                )).toList();

        return ResponseEntity.ok(stations);
    }


}
