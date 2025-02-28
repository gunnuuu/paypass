package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Data
public class BusTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String routeId;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private String arrivalTime;

    @Column(nullable = false)
    private String stationId;
}