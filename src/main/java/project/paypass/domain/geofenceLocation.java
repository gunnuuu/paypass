package project.paypass.domain;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
public class GeofenceLocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainId;

    @Column(nullable = false)
    private LocalDateTime fenceInTime;

    @Column(nullable = false)
    private LocalDateTime fenceOutTime;

    @Column(nullable = false)
    private Long stationNumber;

    @Column(nullable = false)
    private boolean board;

    public GeofenceLocation(String main_id, Long stationNumber) {
        this.mainId = mainId;
        this.fenceInTime = LocalDateTime.now();
        this.fenceOutTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        this.stationNumber = stationNumber;
        this.board = false;
    }

    public void userFenceOut(){
        this.fenceOutTime = LocalDateTime.now();
    }

    public void userBoardBus(){
        this.board = true;
    }
}