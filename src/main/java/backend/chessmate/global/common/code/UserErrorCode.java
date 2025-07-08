package backend.chessmate.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NOT_SUPPORT_GAME_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 게임 타입입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
