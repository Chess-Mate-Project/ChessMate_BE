package backend.chessmate.global.user.service;

import backend.chessmate.global.auth.config.UserPrincipal;
import backend.chessmate.global.auth.dto.response.UserAccountResponse;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.response.TierResponse;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final LichessUtil lichessUtil;
    private final TierUtil tierUtil;

    @Value("${spring.data.redis.key.account_key}")
    private String REDIS_ACCOUNT_KEY;

    public TierResponse getUserTier(GameType gameType, UserPrincipal u) {

        User user = u.getUser();


        // 레디스에 UserAccount가 캐싱 되어 있지 않으면 캐싱 후 조회
        if (!redisService.hasKey(REDIS_ACCOUNT_KEY + user.getLichessId())) {
            lichessUtil.getUserAccount(user.getLichessId());
        }
        //이미 캐싱 되어있으므로 레디스 내에서 조회한다.
        UserAccountResponse account = redisService.get(REDIS_ACCOUNT_KEY + user.getLichessId(), UserAccountResponse.class);

        return switch (gameType) {
            case RAPID -> tierUtil.calculateTier(account.getPerfs().getRapid().getRating());
            case BLITZ -> tierUtil.calculateTier(account.getPerfs().getBlitz().getRating());
            case BULLET -> tierUtil.calculateTier(account.getPerfs().getBullet().getRating());
            case CLASSICAL -> tierUtil.calculateTier(account.getPerfs().getClassical().getRating());
            default -> throw new UserException(UserErrorCode.NOT_SUPPORT_GAME_TYPE); //방어코드~~~ 리퀘스트 @Valid로 검증필요함 아마도
        };

    }

}
