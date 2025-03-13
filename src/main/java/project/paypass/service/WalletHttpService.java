package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.dto.WalletDto;
import project.paypass.repository.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletHttpService {

    private final WalletRepository walletRepository; // DB에서 Wallet 데이터를 관리하는 Repository

    @Transactional
    public void plusAccount(WalletDto walletDto) {
        String mainId = walletDto.getMainId(); // 사용자 ID
        Long change = walletDto.getChange(); // 충전할 금액

        int updatedRows = walletRepository.updateWalletBalance(mainId, change.intValue()); // 잔액 증가

        if (updatedRows == 0) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + mainId);
        }
    }

    @Transactional
    public void minusAccount(WalletDto walletDto) {
        String mainId = walletDto.getMainId(); // 사용자 ID
        Long change = walletDto.getChange(); // 차감할 금액

        // 현재 잔액 조회
        Long currentBalance = getAccount(mainId);

        if (currentBalance < change) {
            throw new IllegalStateException("잔액이 부족합니다."); // 잔액 부족 예외
        }

        int updatedRows = walletRepository.updateWalletBalance(mainId, -change.intValue()); // 잔액 차감

        if (updatedRows == 0) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + mainId);
        }
    }

    public Long getAccount(String mainId) {
        return walletRepository.findByMainId(mainId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + mainId))
                .getAccount(); // 잔액 반환
    }
}
