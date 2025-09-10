package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class SuccessResponseUserPerfResponse {
    private boolean success;
    private String message;
    private UserPerfResponse data;
}