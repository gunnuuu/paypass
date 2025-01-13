package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.user;

import java.util.Optional;

public interface UserRepository extends JpaRepository<user, Long> {
    Optional<user> findBymainid(String mainid);  // mainid로 사용자 조회
    boolean existsBymainid(String mainid);       // mainid로 존재 여부 확인
}
