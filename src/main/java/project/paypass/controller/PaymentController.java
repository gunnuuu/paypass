package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import project.paypass.domain.Payment;
import project.paypass.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/save")
    public ResponseEntity<?> savePayment(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String tid = request.get("tid");
        int amount = Integer.parseInt(request.get("amount"));

        System.out.println("결제 정보 - userId: " + userId + ", tid: " + tid + ", amount: " + amount);

        Payment payment = paymentService.savePayment(userId, tid, amount);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refundPayment(@RequestBody Map<String, String> request) {
        String tid = request.get("tid");

        Payment payment = paymentService.getPaymentByTid(tid).orElseThrow(() ->
                new RuntimeException("해당 결제 내역이 없습니다.")
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "9f3abc05388ac66ac91dc0b3e0c81020"); // ✅ 여기에 카카오페이 Admin Key 입력
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME"); // ✅ 테스트용 CID (실제 환경에서는 다른 값 사용)
        params.add("tid", tid);
        params.add("cancel_amount", String.valueOf(payment.getAmount()));

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/cancel", requestEntity, String.class
        );

        System.out.println("카카오페이 응답: " + response.getBody());

        return ResponseEntity.ok(response.getBody());
    }
}
