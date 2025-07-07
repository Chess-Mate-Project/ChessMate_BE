package backend.chessmate.global.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuthValueRequest {
    private String code;
    private String codeVerifier;

}
