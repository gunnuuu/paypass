package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import project.paypass.domain.DetailLog;

import java.util.List;

public interface DetailLogRepository extends JpaRepository<DetailLog, Long> {
    @Query("SELECT dl FROM DetailLog dl JOIN FETCH dl.log l WHERE l.mainId = :mainId AND l.id = :logId")
    List<DetailLog> findByLog_Main_IdAndLog_Id(String mainId, Long logId);
}
