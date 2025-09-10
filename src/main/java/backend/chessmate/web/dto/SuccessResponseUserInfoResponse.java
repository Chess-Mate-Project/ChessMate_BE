package backend.chessmate.web.dto;

import backend.chessmate.global.user.dto.response.UserInfoResponse;
import lombok.Data;

@Data
public class SuccessResponseUserInfoResponse {
    private boolean success;
    private String message;
    private UserInfoResponse data;
}