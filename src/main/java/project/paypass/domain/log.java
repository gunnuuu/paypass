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
    private Long departureStationNumber;

    @Column(nullable = false)
    private Long arrivalStationNumber;

    @Column(nullable = false)
    PayCheck payCheck;

    public Log(String mainId, LocalDateTime departureTime, LocalDateTime arrivalTime, Long departureStationNumber, Long arrivalStationNumber) {
        this.mainId = mainId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.departureStationNumber = departureStationNumber;
        this.arrivalStationNumber = arrivalStationNumber;
        this.payCheck = PayCheck.False;
    }
}