package project.paypass.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class userLocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainid;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private LocalDateTime time;

    public userLocation(String mainid, double longitude, double latitude) {
        this.mainid = mainid;
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = LocalDateTime.now();
    }
}