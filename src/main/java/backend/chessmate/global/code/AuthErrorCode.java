package backend.chessmate.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    /*
        400 BAD_REQUEST
     */
    UNSUPPORTED_NULL_PROVIDER(HttpStatus.BAD_REQUEST, "소셜 로그인 값이 비어있습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자 입니다."),
    IS_EMPTY_OAUTH_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "소셜로그인 토큰이 비어있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "리프레쉬토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, "리프레쉬 토큰이 일치하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "리프레쉬토큰을 찾을 수 업습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
