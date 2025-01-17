package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.User;
import project.paypass.domain.Wallet;
import project.paypass.repository.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void save(Wallet wallet){
        walletRepository.save(wallet);
    }

    @Transactional
    public Wallet findById(Long id){
        return walletRepository.findById(id).get();
    }

    @Transactional
    public Long findIdByMainId(String mainId){
        return walletRepository.findByMainId(mainId).get(0).getId();
    }
}