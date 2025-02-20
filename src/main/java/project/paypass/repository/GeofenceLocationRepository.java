package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.paypass.domain.GeofenceLocation;

import java.util.List;

public interface GeofenceLocationRepository extends JpaRepository<GeofenceLocation, Long> {

    @Query("select g from GeofenceLocation g where g.mainId = :mainId AND g.stationNumber = :stationNumber")
    List<GeofenceLocation> findByMainIdAndStationNumber(@Param("mainId")String mainId, @Param("stationNumber")Long stationNumber);

    List<GeofenceLocation> findByMainId(String mainId);
}