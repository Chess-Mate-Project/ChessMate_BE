package backend.chessmate.global.common.code;

import org.springframework.http.HttpStatus;

public interface SuccessCode {
    HttpStatus getHttpStatus();
    String getMessage();
}
