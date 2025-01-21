package project.paypass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.paypass.domain.dto.WalletDto;
import project.paypass.service.WalletHttpService;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletHttpService walletHttpService;

    @GetMapping("/mypage/account")
    public ResponseEntity<Long> getAccount(@RequestParam String mainId) {
        Long accountBalance = walletHttpService.getAccount(mainId);
        return ResponseEntity.ok(accountBalance);
    }

    @PostMapping("/mypage/update")
    public ResponseEntity<Void> updateAccount(@RequestBody WalletDto walletDto){
        if (walletDto.getMainId() == null) {
            throw new IllegalArgumentException("mainId가 null입니다.");
        }
        walletHttpService.updateAccount(walletDto);
        return ResponseEntity.ok().build();
    }
}
