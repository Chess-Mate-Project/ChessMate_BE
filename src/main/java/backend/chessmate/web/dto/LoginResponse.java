package backend.chessmate.web.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String type;
    private String accessToken;
    private String refreshToken;
}
