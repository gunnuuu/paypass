package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import project.paypass.domain.PayCheck;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class LogDto {
    private Long id;
    private String mainId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Long departureStationNumber;
    private Long arrivalStationNumber;
    private PayCheck payCheck;

    public LogDto(Long id, String mainId, LocalDateTime departureTime, LocalDateTime arrivalTime, Long departureStationNumber, Long arrivalStationNumber, PayCheck payCheck) {
        this.id = id;
        this.mainId = mainId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.departureStationNumber = departureStationNumber;
        this.arrivalStationNumber = arrivalStationNumber;
        this.payCheck = payCheck;
    }
}