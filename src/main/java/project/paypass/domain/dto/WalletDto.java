package project.paypass.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class WalletDto {
    private String mainId;
    private Long account;
    private Long change;
    private String choice;
    public WalletDto(String mainId, Long account, Long change, String choice) {
        this.mainId = mainId;
        this.account = account;
        this.change = change;
        this.choice = choice;
    }
}