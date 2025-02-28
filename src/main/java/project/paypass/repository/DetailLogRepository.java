package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.DetailLog;

public interface DetailLogRepository extends JpaRepository<DetailLog, Long> {
}