package backend.chessmate.global.response;

import lombok.Getter;


@Getter
public class ErrorResponse{
    private final boolean success;
    private final String message;

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
    }
}
