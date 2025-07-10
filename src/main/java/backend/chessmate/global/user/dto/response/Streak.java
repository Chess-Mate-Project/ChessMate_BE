package backend.chessmate.global.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class Streak {
    private LocalDate date;
    private int PlayCount;
//    private int WinCount;
//    private int LossCount;
}
