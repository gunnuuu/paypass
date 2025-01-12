package project.paypass.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class log {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String main_id;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private Long departureStation;

    @Column(nullable = false)
    private Long arrivalStation;

    @Column(nullable = false)
    boolean payCheck;

    public log(String main_id, LocalDateTime departureTime, LocalDateTime arrivalTime, Long departureStation, Long arrivalStation) {
        this.main_id = main_id;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.payCheck = false;
    }
}