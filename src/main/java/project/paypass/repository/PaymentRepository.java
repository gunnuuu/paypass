package project.paypass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.paypass.domain.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTid(String tid);
}