package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class user {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mainid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false)
    private String phoneNumber;

    public user(String mainid, String name, LocalDate birth, String phoneNumber) {
        this.mainid = mainid;
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
    }

    public user() {

    }

    public void setMainId(String email) {
        this.mainid = email;
    }

    public String getMainId() {
        return mainid;
    }
}