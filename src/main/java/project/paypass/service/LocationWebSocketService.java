package project.paypass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.UserLocation;
import project.paypass.domain.dto.UserLocationDto;

@Service
@RequiredArgsConstructor
public class LocationWebSocketService {

    private final UserLocationService userLocationService;

    public void saveUserLocation(UserLocationDto userLocationDto){
        String mainId = userLocationDto.getMainId();
        double longitude = userLocationDto.getLongitude();
        double latitude = userLocationDto.getLatitude();

        userLocationService.save(new UserLocation(mainId,longitude,latitude));

    }
}