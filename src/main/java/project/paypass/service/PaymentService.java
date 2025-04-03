package project.paypass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.Payment;
import project.paypass.domain.PaymentStatus;
import project.paypass.repository.PaymentRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    // 결제 정보 저장
    public Payment savePayment(String userId, String tid, int amount) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setTid(tid);
        payment.setAmount(amount);

        payment.setStatus(PaymentStatus.SUCCESS);


        return paymentRepository.save(payment);
    }

    // TID로 결제 정보 조회
    public Optional<Payment> getPaymentByTid(String tid) {
        return paymentRepository.findByTid(tid);
    }
}
