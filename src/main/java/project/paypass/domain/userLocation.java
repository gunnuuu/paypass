package project.paypass.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class userLocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String main_id;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private LocalDateTime time;

    public userLocation(String main_id, double longitude, double latitude) {
        this.main_id = main_id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = LocalDateTime.now();
    }
}