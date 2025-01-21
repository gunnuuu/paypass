package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.Wallet;
import project.paypass.domain.dto.WalletDto;
import project.paypass.repository.WalletRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletHttpService {

    private final WalletRepository walletRepository; // DB에서 Wallet 데이터를 관리하는 Repository

    @Transactional
    public void updateAccount(WalletDto walletDto) {
        String mainId = walletDto.getMainId(); // 사용자 ID
        Long change = walletDto.getChange(); // 변경할 금액
        String choice = walletDto.getChoice(); // "deposit" 또는 "withdraw"

        // 사용자 Wallet 조회 (가장 마지막 wallet을 가져오기 위해 .stream().findFirst() 사용)
        Wallet wallet = walletRepository.findByMainId(mainId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + mainId)); // 없으면 예외

        // 지갑 잔액 업데이트 로직
        Long newAccountBalance;
        if ("deposit".equalsIgnoreCase(choice)) {
            newAccountBalance = wallet.getAccount() + change; // 충전
        } else if ("withdraw".equalsIgnoreCase(choice)) {
            if (wallet.getAccount() < change) {
                throw new IllegalArgumentException("잔액이 부족합니다."); // 출금 실패 처리
            }
            newAccountBalance = wallet.getAccount() - change; // 출금
        } else {
            throw new IllegalArgumentException("유효하지 않은 작업입니다: " + choice);
        }

        // 기존 Wallet 객체의 값을 새로운 생성자를 사용해 업데이트
        Wallet updatedWallet = new Wallet(wallet.getId(), wallet.getMainId(), newAccountBalance, wallet.getCharge(), wallet.isPayCheck());

        // Wallet 정보 업데이트
        walletRepository.save(updatedWallet); // 기존 Wallet 객체를 업데이트
    }

    public Long getAccount(String mainId) {
        Wallet wallet = walletRepository.findByMainId(mainId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + mainId)); // 없으면 예외
        return wallet.getAccount(); // account 값 반환
    }
}
