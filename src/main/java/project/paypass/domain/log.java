package project.paypass.domain;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
public class Log {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainId;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private Long departureStation;

    @Column(nullable = false)
    private Long arrivalStation;

    @Column(nullable = false)
    PayCheck payCheck;

    public Log(String mainId, LocalDateTime departureTime, LocalDateTime arrivalTime, Long departureStation, Long arrivalStation) {
        this.mainId = mainId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.payCheck = PayCheck.False;
    }
}