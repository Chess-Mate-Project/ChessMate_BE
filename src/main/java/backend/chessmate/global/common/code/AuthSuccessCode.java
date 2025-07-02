package backend.chessmate.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {

    LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공");

    private final HttpStatus httpStatus;
    private final String message;


}