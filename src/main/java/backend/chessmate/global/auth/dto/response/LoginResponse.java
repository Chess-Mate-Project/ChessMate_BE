package backend.chessmate.global.auth.dto.response;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String type;
    private String accessToken;
    private String refreshToken;
}
