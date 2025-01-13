package project.paypass.domain;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

@Entity
public class login {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mainid;

    @Column(nullable = false)
    private String password;

    public login(String mainid, String password) {
        this.mainid = mainid;
        this.password = password;
    }
}