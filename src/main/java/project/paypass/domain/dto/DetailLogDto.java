package project.paypass.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.paypass.domain.PayCheck;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DetailLogDto {
    private Long id;
    private String mainId;
    private LocalDateTime fenceInTime;
    private LocalDateTime fenceOutTime;
    private Long stationNumber;
}
