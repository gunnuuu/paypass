package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.Log;

public interface LogRepository extends JpaRepository<Log, Long> {
}