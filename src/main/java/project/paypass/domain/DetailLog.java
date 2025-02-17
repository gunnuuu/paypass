package project.paypass.domain;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
public class DetailLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainId;

    @Column(nullable = false)
    private Long logId;

    @Column(nullable = false)
    private LocalDateTime fenceInTime;

    @Column(nullable = false)
    private LocalDateTime fenceOutTime;

    @Column(nullable = false)
    private Long stationNumber;

}