package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.paypass.domain.dto.WalletDto;
import project.paypass.service.WalletHttpService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletHttpService walletHttpService;

    @PostMapping("/mypage/account")
    public ResponseEntity<Long> getAccount(@RequestBody Map<String, String> request) {
        String mainId = request.get("email");  // JSON에서 email 가져오기
        Long accountBalance = walletHttpService.getAccount(mainId);
        return ResponseEntity.ok(accountBalance);
    }

    @PostMapping("/mypage/plus")
    public ResponseEntity<Void> plusAccount(@RequestBody WalletDto walletDto){
        if (walletDto.getMainId() == null) {
            throw new IllegalArgumentException("mainId가 null입니다.");
        }
        walletHttpService.plusAccount(walletDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mypage/minus")
    public ResponseEntity<Void> minusAccount(@RequestBody WalletDto walletDto){
        if (walletDto.getMainId() == null) {
            throw new IllegalArgumentException("mainId가 null입니다.");
        }
        walletHttpService.minusAccount(walletDto);
        return ResponseEntity.ok().build();
    }
}
