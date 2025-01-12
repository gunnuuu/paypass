package project.paypass.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class user {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String main_id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime birth;

    @Column(nullable = false)
    private String phoneNumber;

    public user(String main_id, String name, LocalDateTime birth, String phoneNumber) {
        this.main_id = main_id;
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
    }
}