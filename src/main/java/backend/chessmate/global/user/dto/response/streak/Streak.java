package backend.chessmate.global.user.dto.response.streak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class Streak {
    private LocalDate date;
    private int winCount;
    private int loseCount;
    private int drawCount;
}
