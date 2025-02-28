package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.BusTime;

import java.util.List;

public interface BusTimeRepository extends JpaRepository<BusTime, Long> {
    List<BusTime> findByRouteId(String routeId);
}