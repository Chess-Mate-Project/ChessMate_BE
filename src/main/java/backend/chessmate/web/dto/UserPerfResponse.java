package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class UserPerfResponse {
    private int totalGames;
    private double winRate;
}