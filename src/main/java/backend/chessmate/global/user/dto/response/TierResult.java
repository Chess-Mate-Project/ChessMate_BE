package backend.chessmate.global.user.dto.response;

import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.entity.SubTierType;
import backend.chessmate.global.user.entity.TierType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TierResult {
    private TierType tier;
    private SubTierType subTier;
    private int rating;
}