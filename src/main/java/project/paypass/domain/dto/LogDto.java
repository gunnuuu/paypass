package project.paypass.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.paypass.domain.PayCheck;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LogDto {
    private Long id;
    private String mainId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Long departureStationNumber;
    private Long arrivalStationNumber;
    private String routeIdList;
    private PayCheck payCheck;
}
