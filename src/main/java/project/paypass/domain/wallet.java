package project.paypass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class wallet {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String main_id;

    @Column(nullable = false)
    private Long account;

    @Column(nullable = false)
    private Long charge;

    @Column(nullable = false)
    private boolean payCheck;

    public wallet(String main_id) {
        this.main_id = main_id;
        this.account = 0L;
        this.charge = 0L;
        this.payCheck = true;
    }
}