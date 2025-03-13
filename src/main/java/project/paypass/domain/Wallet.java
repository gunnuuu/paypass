package project.paypass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Getter
@Setter
public class Wallet {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String mainId;

    @Column(nullable = false)
    private Long account;

    @Column(nullable = false)
    private Long charge;

    @Column(nullable = false)
    private boolean payCheck;

    public Wallet(String mainId) {
        this.mainId = mainId;
        this.account = 0L;
        this.charge = 0L;
        this.payCheck = true;
    }

    public Wallet(Long id, String mainId, Long account, Long charge, boolean payCheck) {
        this.id = id;
        this.mainId = mainId;
        this.account = account;
        this.charge = charge;
        this.payCheck = payCheck;
    }
}
