package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.paypass.domain.Station;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {

    @Query("select s.busInfo from Station s where s.stationNumber = :stationNumber")
    String findBusInfoByStationNumber(@Param("stationNumber")Long stationNumber);

    List<Station> findByStationNumberIn(List<Long> stationNumbers);
}

