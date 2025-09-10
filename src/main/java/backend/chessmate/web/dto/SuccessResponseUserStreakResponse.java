package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class SuccessResponseUserStreakResponse {
    private boolean success;
    private String message;
    private UserStreakResponse data;
}