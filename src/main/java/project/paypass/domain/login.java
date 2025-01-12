package project.paypass.domain;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

@Entity
public class login {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String main_id;

    @Column(nullable = false)
    private String password;

    public login(String main_id, String password) {
        this.main_id = main_id;
        this.password = password;
    }
}