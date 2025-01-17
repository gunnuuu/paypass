package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.UserLocation;
import project.paypass.repository.UserLocationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserLocationService {

    private final UserLocationRepository userLocationRepository;

    @Transactional
    public void save(UserLocation userLocation){
        userLocationRepository.save(userLocation);
    }

    @Transactional
    public List<UserLocation> findByMainId(String mainId){
        return userLocationRepository.findByMainId(mainId);
    }

}