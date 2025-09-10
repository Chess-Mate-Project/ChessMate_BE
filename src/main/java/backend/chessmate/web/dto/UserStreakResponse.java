package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class UserStreakResponse {
    private java.util.List<UserStreak> streaks;
}