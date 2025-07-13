package backend.chessmate.global.user.dto.response;

import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.dto.response.tier.TierResult;
import backend.chessmate.global.user.utils.TierUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Mono;

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

    public static Mono<UserPerfResponse> from(Mono<UserPerf> perf, TierUtil tierUtil) {

        return perf.map(p -> {
                    var stat = p.getStat();
                    Integer highestRating = stat.getHighest().getRating();
                    Integer lowestRating = stat.getLowest().getRating();

                    var glicko = p.getPerf().getGlicko();
                    Integer nowRating = (int) glicko.getRating();

                    var count = stat.getCount();
                    Integer totalGames = count.getAll();
                    Integer winCount = count.getWin();
                    Integer lossCount = count.getLoss();
                    Integer drawCount = count.getDraw();
                    Long playTime = count.getSeconds();

                    var streak = stat.getResultStreak();
                    Integer maxWinningStreak = streak.getWin().getMax().getValue();
                    Integer maxLosingStreak = streak.getLoss().getMax().getValue();
                    Integer nowWinningStreak = streak.getWin().getCur().getValue();
                    Integer nowLosingStreak = streak.getLoss().getCur().getValue();

                    Double percentile = p.getPercentile();

                    TierResult nowTierResult = tierUtil.calculateTier(nowRating);
                    TierResult maxTierResult = tierUtil.calculateTier(highestRating);
                    TierResult minTierResult = tierUtil.calculateTier(lowestRating);

                    double winRate = 0.0;
                    if (totalGames != null && totalGames > 0 && winCount != null) {
                        winRate = (double) winCount / totalGames * 100;
                    }

                    return UserPerfResponse.builder()
                            .nowTier(nowTierResult)
                            .maxTier(maxTierResult)
                            .minTier(minTierResult)
                            .totalGames(totalGames)
                            .playTime(playTime)
                            .percentile(percentile)
                            .winCount(winCount)
                            .lossCount(lossCount)
                            .drawCount(drawCount)
                            .winRate(winRate)
                            .maxWinningStreak(maxWinningStreak)
                            .maxLosingStreak(maxLosingStreak)
                            .nowWinningStreak(nowWinningStreak)
                            .nowLosingStreak(nowLosingStreak)
                            .build();


                }

        );

    }
}


