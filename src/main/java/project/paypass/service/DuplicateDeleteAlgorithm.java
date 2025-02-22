package project.paypass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.paypass.domain.GeofenceLocation;

import java.util.List;

@Slf4j
@Service
public class DuplicateDeleteAlgorithm {

    public List<GeofenceLocation> algorithmStart(List<GeofenceLocation> geofenceLocations){

        return geofenceLocations;
    }
}