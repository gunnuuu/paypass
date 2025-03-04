package project.paypass.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@Getter
public class DetailLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mainId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @Column(nullable = false)
    private LocalDateTime fenceInTime;

    private LocalDateTime fenceOutTime;

    @Column(nullable = false)
    private Long stationNumber;

    public DetailLog(String mainId, Log log, LocalDateTime fenceInTime, LocalDateTime fenceOutTime, Long stationNumber) {
        this.mainId = mainId;
        this.log = log;
        this.fenceInTime = fenceInTime;
        this.fenceOutTime = fenceOutTime;
        this.stationNumber = stationNumber;
    }

    @Override
    public String toString() {
        return "DetailLog{" +
                "mainId='" + mainId + '\'' +
                ", log=" + log +
                ", stationNumber=" + stationNumber +
                '}';
    }
}