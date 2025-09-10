package backend.chessmate.web.dto;


import lombok.Data;

@Data
public class OAuthValueRequest {
    private String code;
    private String codeVerifier;

    public OAuthValueRequest(String code, String codeVerifier) {
        this.code = code;
        this.codeVerifier = codeVerifier;
    }
}
