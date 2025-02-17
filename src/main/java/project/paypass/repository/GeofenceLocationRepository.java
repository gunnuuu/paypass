package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.paypass.domain.GeofenceLocation;

import java.time.LocalDateTime;
import java.util.List;

public interface GeofenceLocationRepository extends JpaRepository<GeofenceLocation, Long> {

    // 1️⃣ 특정 사용자의 mainId와 stationNumber에 해당하는 Geofence 데이터 조회
    @Query("SELECT g FROM GeofenceLocation g WHERE g.mainId = :mainId AND g.stationNumber = :stationNumber")
    List<GeofenceLocation> findByMainIdAndStationNumber(@Param("mainId") String mainId, @Param("stationNumber") Long stationNumber);

    // 2️⃣ 특정 mainId에 대해 진입한 모든 Geofence 데이터를 시간 순으로 정렬하여 조회
    @Query("SELECT g FROM GeofenceLocation g WHERE g.mainId = :mainId ORDER BY g.fenceInTime ASC")
    List<GeofenceLocation> findByMainId(@Param("mainId") String mainId);

    // 3️⃣ 특정 mainId에 대한 bus_info 목록 조회 (String 형태로 반환)
    @Query("SELECT g.busInfo FROM GeofenceLocation g WHERE g.mainId = :mainId")
    List<String> findBusInfoByMainId(@Param("mainId") String mainId);

    // 4️⃣ 특정 routeId와 sequence에 해당하는 가장 최근의 fenceInTime 조회 (이전 정류장 시간 가져오기)
    @Query("SELECT g.fenceInTime FROM GeofenceLocation g WHERE g.mainId = :mainId AND g.busInfo LIKE CONCAT('%{', :routeId, ',', :sequence, '}%') ORDER BY g.fenceInTime DESC")
    List<LocalDateTime> findEntryTime(@Param("mainId") String mainId, @Param("routeId") int routeId, @Param("sequence") int sequence);

    // 5️⃣ 특정 routeId와 sequence에 해당하는 가장 최근의 fenceOutTime 조회 (이전 정류장 시간 가져오기)
    @Query("SELECT g.fenceOutTime FROM GeofenceLocation g WHERE g.mainId = :mainId AND g.busInfo LIKE CONCAT('%{', :routeId, ',', :sequence, '}%') ORDER BY g.fenceInTime DESC")
    List<LocalDateTime> findExitTime(@Param("mainId") String mainId, @Param("routeId") int routeId, @Param("sequence") int sequence);

    // 6️⃣ 특정 routeId와 sequence에 해당하는 가장 최근의 GeofenceLocation 조회
    @Query("SELECT g FROM GeofenceLocation g WHERE g.mainId = :mainId AND g.busInfo LIKE CONCAT('%{', :routeId, ',', :sequence, '}%')")
    GeofenceLocation findByMainIdAndRouteIdAndSequence(@Param("mainId") String mainId,
                                                       @Param("routeId") int routeId,
                                                       @Param("sequence") int sequence);

}
