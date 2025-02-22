package project.paypass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.BusTime;
import project.paypass.repository.BusTimeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusTimeService {

    private final BusTimeRepository busTimeRepository;

    public void saveBusTimes(List<BusTime> busTimes) {
        busTimeRepository.saveAll(busTimes);
    }
}
