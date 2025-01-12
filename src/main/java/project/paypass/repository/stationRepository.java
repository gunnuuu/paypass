package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.station;

public interface stationRepository extends JpaRepository<station, Long> {
}
