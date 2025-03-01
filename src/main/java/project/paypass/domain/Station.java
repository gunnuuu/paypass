package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Getter
public class Station {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long stationNumber;

    private double longitude;

    private double latitude;

    @Column(columnDefinition = "TEXT")
    private String busInfo;
}
