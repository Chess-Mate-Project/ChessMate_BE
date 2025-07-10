package backend.chessmate.global.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class GamesInUserInfo {
    private String opening;
    private String firstMove;
}
