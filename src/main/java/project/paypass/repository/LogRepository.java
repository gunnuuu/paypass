package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import project.paypass.domain.Log;
import java.util.List;

public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findByMainId(@Param("mainId")String mainId);
}