package backend.chessmate.global.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class UserPerfResponse {

    private TierResult nowTier;
    private TierResult maxTier;
    private TierResult minTier;


    private Integer totalGames;
    private Long playTime;//초 기준
    private Double percentile; //상위 몇프로인지? 백분율

    private Integer winCount;
    private Integer lossCount;
    private Integer drawCount;
    private Double winRate;

    private Integer maxWinningStreak; //최대 연승
    private Integer maxLosingStreak; //최대 연패

    private Integer nowWinningStreak; //현재 연승
    private Integer nowLosingStreak; //현재 연패

}
