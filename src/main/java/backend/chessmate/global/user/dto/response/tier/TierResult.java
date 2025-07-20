package backend.chessmate.global.user.dto.response.tier;

import backend.chessmate.global.user.entity.SubTierType;
import backend.chessmate.global.user.entity.TierType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TierResult {
    private TierType tier;
    private SubTierType subTier;
    private int rating;
}