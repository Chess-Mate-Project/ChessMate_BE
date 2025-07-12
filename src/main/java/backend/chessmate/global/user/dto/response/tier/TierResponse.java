package backend.chessmate.global.user.dto.response.tier;

import backend.chessmate.global.user.entity.GameType;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class TierResponse {
    private String userName;
    private GameType gameType;
    private TierResult result;

}
