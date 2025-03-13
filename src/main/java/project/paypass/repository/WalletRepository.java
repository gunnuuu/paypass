package project.paypass.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.paypass.domain.Wallet;

import java.util.List;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByMainId(@Param("mainId")String mainId);

    @Modifying
    @Transactional
    @Query("UPDATE Wallet w SET w.account = w.account + :amount WHERE w.mainId = :mainId")
    int updateWalletBalance(@Param("mainId") String mainId, @Param("amount") int amount);
}