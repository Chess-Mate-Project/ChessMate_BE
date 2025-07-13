package backend.chessmate.global.user.dto.response.streak;

import backend.chessmate.global.user.entity.Streak;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class UserStreak {
    private LocalDate date;
    private int winCount;
    private int loseCount;
    private int drawCount;

    public static UserStreak from(Streak streak) {
        return UserStreak.builder()
                .date(streak.getDate())
                .winCount(streak.getWinCount())
                .loseCount(streak.getLoseCount())
                .drawCount(streak.getDrawCount())
                .build();
    }
}
