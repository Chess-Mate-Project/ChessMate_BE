package backend.chessmate.global.user.dto.response;

import backend.chessmate.global.user.entity.SubTierType;
import backend.chessmate.global.user.entity.TierType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class TierResponse {
    private TierType tier;
    private SubTierType subTier;
    private int rating;

}
