package backend.chessmate.global.user.dto;

import backend.chessmate.global.user.entity.SubTierType;
import backend.chessmate.global.user.entity.TierType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class TierResult {
    private TierType tier;
    private SubTierType level;
    private int rating;

}
