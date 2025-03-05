package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.paypass.domain.Station;
import project.paypass.domain.dto.DetailLogDto;
import project.paypass.domain.dto.StationDto;
import project.paypass.service.StationService;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping("/getStations")
    public ResponseEntity<List<StationDto>> getStations() {
        List<StationDto> stations = stationService.findAll().stream()
                .map(station -> new StationDto(
                        station.getName(),
                        station.getStationNumber(),
                        station.getLatitude(),
                        station.getLongitude()
                )).toList();

        return ResponseEntity.ok(stations);
    }

    @PostMapping("/getStationLocations")
    public ResponseEntity<List<StationDto>> getStationLocations(@RequestBody Map<String, Object> request) {
        // stationNumbers만 받음
        List<Long> stationNumbers = (List<Long>) request.get("stationNumbers");

        // stationService에서 StationDto 리스트 반환
        List<StationDto> stationLocations = stationService.findLocationsByStationNumbers(stationNumbers);

        return ResponseEntity.ok(stationLocations);
    }
}

