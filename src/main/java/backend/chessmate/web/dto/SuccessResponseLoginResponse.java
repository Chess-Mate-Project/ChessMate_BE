package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class SuccessResponseLoginResponse {
    private boolean success;
    private String message;
    private LoginResponse data;
}
