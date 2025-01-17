package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.Station;

public interface StationRepository extends JpaRepository<Station, Long> {
}