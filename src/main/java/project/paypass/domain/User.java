package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@Getter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mainId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime birth;

    @Column(nullable = false)
    private String phoneNumber;

    public User(String mainId, String name, LocalDateTime birth, String phoneNumber) {
        this.mainId = mainId;
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
    }
}
