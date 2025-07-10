package backend.chessmate.global.user.dto.response;

import backend.chessmate.global.user.entity.BannerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private String userName;
    private BannerType banner;
    private int profile; //일단 정수타입
    private String intro;

    private String tactic;
    private String firstMove;
}
