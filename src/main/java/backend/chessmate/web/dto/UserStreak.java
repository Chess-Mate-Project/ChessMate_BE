package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class UserStreak {
    private String date;
    private int winCount;
    private int loseCount;
    private int drawCount;
}