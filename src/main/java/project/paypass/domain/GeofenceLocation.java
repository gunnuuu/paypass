package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@ToString
public class GeofenceLocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainId;

    @Getter
    @Column(nullable = false)
    private LocalDateTime fenceInTime;

    @Getter
    private LocalDateTime fenceOutTime;

    @Getter
    @Column(nullable = false)
    private Long stationNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String busInfo;


    public GeofenceLocation(String mainId, Long stationNumber, String busInfo) {
        this.mainId = mainId;
        this.fenceInTime = LocalDateTime.now();
        this.fenceOutTime = null;
        this.stationNumber = stationNumber;
        this.busInfo = busInfo;
    }

    public void userFenceOut(){
        this.fenceOutTime = LocalDateTime.now();
    }

    public boolean fenceOutTimeIsNull(){
        return fenceOutTime == null;
    }

    public LocalDateTime userFenceInTime(){
        return this.fenceInTime;
    }

    public String stationBusInfo(){
        return this.busInfo;
    }

    @Override
    public String toString() {
        return "GeofenceLocation{" +
                "mainId='" + mainId + '\'' +
                ", stationNumber=" + stationNumber +
                '}';
    }
}
