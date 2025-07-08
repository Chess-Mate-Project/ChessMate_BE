package backend.chessmate.global.user.utils;

import backend.chessmate.global.user.dto.response.TierResponse;
import backend.chessmate.global.user.entity.SubTierType;
import backend.chessmate.global.user.entity.TierType;
import org.springframework.stereotype.Component;

@Component
public class TierUtil {

    public TierResponse calculateTier(int rating) {
        if (rating < 400) {
            return new TierResponse(TierType.UNRATED, SubTierType.UNRATED, rating);
        }

        if (rating <= 900) {
            return new TierResponse(TierType.PAWN, getSubTier(rating, 400, 900), rating);
        } else if (rating <= 1200) {
            return new TierResponse(TierType.KNIGHT, getSubTier(rating, 901, 1200), rating);
        } else if (rating <= 1500) {
            return new TierResponse(TierType.BISHOP, getSubTier(rating, 1201, 1500), rating);
        } else if (rating <= 1800) {
            return new TierResponse(TierType.ROOK, getSubTier(rating, 1501, 1800), rating);
        } else if (rating <= 2100) {
            return new TierResponse(TierType.QUEEN, getSubTier(rating, 1801, 2100), rating);
        } else {
            return new TierResponse(TierType.KING, getSubTier(rating, 2101, 2700), rating);
        }
    }

    public SubTierType getSubTier(int rating, int min, int max) {
        int range = max - min + 1;
        int step = range / 5;

        if (rating < min + step) {
            return SubTierType.V;
        } else if (rating < min + step * 2) {
            return SubTierType.IV;
        } else if (rating < min + step * 3) {
            return SubTierType.III;
        } else if (rating < min + step * 4) {
            return SubTierType.II;
        } else {
            return SubTierType.I;
        }
    }

}
