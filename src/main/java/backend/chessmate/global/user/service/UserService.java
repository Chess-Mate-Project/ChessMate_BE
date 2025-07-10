package backend.chessmate.global.user.service;

import backend.chessmate.global.auth.config.UserPrincipal;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.response.*;
import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final LichessUtil lichessUtil;
    private final TierUtil tierUtil;

//    @Value("${spring.data.redis.key.account_key}")
//    private String REDIS_ACCOUNT_KEY;

    @Value("${spring.data.redis.key.perf_key}")
    private String REDIS_PERF_KEY;

    @Value("${spring.data.redis.key.games_key}")
    private String REDIS_GAMES_KEY;

//    public TierResponse processUserAccount(GameType gameType, UserPrincipal u) {
//
//        User user = u.getUser();
//
//        UserAccountResponse account;
//        // 레디스에 UserAccount가 캐싱 되어 있지 않으면 캐싱 후 조회
//        if (!redisService.hasKey(REDIS_ACCOUNT_KEY + user.getLichessId())) {
//            account = lichessUtil.getUserAccount(user.getLichessId());
//        } else {
//            //이미 캐싱 되어있으므로 레디스 내에서 조회한다.
//            account = redisService.get(REDIS_ACCOUNT_KEY + user.getLichessId(), UserAccountResponse.class);
//
//        }
//
//        int rating = getRatingByGameType(account, gameType);
//        TierResult result = tierUtil.calculateTier(rating);
//
//
//        return TierResponse.builder()
//                .userName(user.getLichessId())
//                .gameType(gameType)
//                .result(result)
//                .build();
//
//    }
//
//    private int getRatingByGameType(UserAccountResponse account, GameType gameType) {
//        return switch (gameType) {
//            case RAPID -> account.getPerfs().getRapid().getRating();
//            case BLITZ -> account.getPerfs().getBlitz().getRating();
//            case BULLET -> account.getPerfs().getBullet().getRating();
//            case CLASSICAL -> account.getPerfs().getClassical().getRating();
//            default -> throw new UserException(UserErrorCode.NOT_SUPPORT_GAME_TYPE);
//        };
//    }

    public UserPerfResponse processUserPerf(GameType gameType, UserPrincipal u) {

        User user = u.getUser();

        UserPerf perf;
        if (!redisService.hasKey(REDIS_PERF_KEY + user.getLichessId() + gameType)) {
            perf = lichessUtil.callUserPerfApi(user, gameType);
        } else {
            perf = redisService.get(REDIS_PERF_KEY + user.getLichessId() + gameType, UserPerf.class);

        }


        if (perf == null) {
            log.error("redis perf is null");
            throw new UserException(UserErrorCode.FAILD_GET_USER_PERF);

        }

        var stat = perf.getStat();
        Integer highestRating = stat.getHighest().getRating();
        Integer lowestRating = stat.getLowest().getRating();

        var glicko = perf.getPerf().getGlicko();
        Integer nowRating = (int) glicko.getRating();

        var count = stat.getCount();
        Integer totalGames = count.getAll();
        Integer winCount = count.getWin();
        Integer lossCount = count.getLoss();
        Integer drawCount = count.getDraw();
        Long playTime = count.getSeconds();

        var streak = stat.getResultStreak();
        Integer maxWinningStreak = streak.getWin().getMax().getValue();
        Integer maxLosingStreak = streak.getLoss().getMax().getValue();
        Integer nowWinningStreak = streak.getWin().getCur().getValue();
        Integer nowLosingStreak = streak.getLoss().getCur().getValue();

        Double percentile = perf.getPercentile();

        // tier 계산
        TierResult nowTierResult = tierUtil.calculateTier(nowRating);
        TierResult maxTierResult = tierUtil.calculateTier(highestRating);
        TierResult minTierResult = tierUtil.calculateTier(lowestRating);

        // 승률 계산
        double winRate = 0.0;
        if (totalGames != null && totalGames > 0 && winCount != null) {
            winRate = (double) winCount / totalGames * 100;
        }

        return UserPerfResponse.builder()
                .nowTier(nowTierResult)
                .maxTier(maxTierResult)
                .minTier(minTierResult)
                .totalGames(totalGames)
                .playTime(playTime)
                .percentile(percentile)
                .winCount(winCount)
                .lossCount(lossCount)
                .drawCount(drawCount)
                .winRate(winRate)
                .maxWinningStreak(maxWinningStreak)
                .maxLosingStreak(maxLosingStreak)
                .nowWinningStreak(nowWinningStreak)
                .nowLosingStreak(nowLosingStreak)
                .build();


    }


    public GamesInUserInfo processUserGamesInUserInfo(UserPrincipal u) {
        log.info("=== getUserGames 호출됨 ===");
        User user = u.getUser();

        String key = REDIS_GAMES_KEY + user.getLichessId();
        UserGames games = null;
        if (redisService.hasKey(key)) {
            log.info("=== Redis 캐시 hit ===");
            games = redisService.get(key, UserGames.class);
        } else {
            log.info("=== Redis 캐시 miss === API 호출 ===");
            games = lichessUtil.callUserGamesApi(u.getUser()).block();
        }
        Map<String, Long> openingMap = new HashMap<>();
        Map<String, Long> firstMoveMap = new HashMap<>();
        for (UserGame game : games.getGames()) {

            String opening = game.getOpening().getName();
            if (opening.contains(":")) {
                opening = opening.split(":")[0].trim();
            }

            openingMap.merge(opening, 1L, Long::sum);

            String firstMove = game.getMoves().split(" ")[0];

            firstMoveMap.merge(firstMove, 1L, Long::sum);
        }

        String opening = openingMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String firstMove = firstMoveMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return GamesInUserInfo.builder()
                .opening(opening)
                .firstMove(firstMove)
                .build();


    }

    public StreaksResponse processUserGamesInYearStreak(UserPrincipal u) {
        log.info("=== getUserGames 호출됨 ===");
        User user = u.getUser();

        String key = REDIS_GAMES_KEY + user.getLichessId();
        UserGames games = null;
        if (redisService.hasKey(key)) {
            log.info("=== Redis 캐시 hit ===");
            games = redisService.get(key, UserGames.class);
        } else {
            log.info("=== Redis 캐시 miss === API 호출 ===");
            games = lichessUtil.callUserGamesApi(u.getUser()).block();
        }

        Map<LocalDate, Streak> streakMap = new HashMap<>();

        for (UserGame game : games.getGames()) {

            LocalDate date = Instant.ofEpochMilli(game.getCreatedAt())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            Streak streak = streakMap.getOrDefault(date, Streak.builder()
                    .date(date)
                    .winCount(0)
                    .loseCount(0)
                    .drawCount(0)
                    .build());

            String status = game.getStatus();


            if (game.getStatus().equals("draw")) {
                streak.setDrawCount(streak.getDrawCount() + 1);

            } else if (game.getStatus().equals("resign")) {
                String winnerColor = game.getWinner();

                if (game.getPlayers().getWhite().getUser().getName().equals(user.getName())) { //유저가 백
                    if (winnerColor.equals("white")) {
                        streak.setWinCount(streak.getWinCount() + 1);
                    } else {
                        streak.setLoseCount(streak.getLoseCount() + 1);
                    }
                } else { //유저가 흑
                    if (winnerColor.equals("black")) {
                        streak.setWinCount(streak.getWinCount() + 1);
                    } else {
                        streak.setLoseCount(streak.getLoseCount() + 1);
                    }
                }
            }
            streakMap.put(date, streak);

        }
        List<Streak> streaks = new ArrayList<>(streakMap.values());
        streaks.sort(Comparator.comparing(Streak::getDate));

        return StreaksResponse.builder()
                .streaks(streaks)
                .build();


    }


        public UserInfoResponse getUserInfo (UserPrincipal u){
            User user = u.getUser();
            String key = REDIS_GAMES_KEY + user.getLichessId();

            GamesInUserInfo gamesInUserInfo = processUserGamesInUserInfo(u);


            return UserInfoResponse.builder()
                    .userName(user.getLichessId())
                    .profile(user.getProfile())
                    .banner(user.getBanner())
                    .intro(user.getIntro())
                    .firstMove(gamesInUserInfo.getFirstMove())
                    .opening(gamesInUserInfo.getOpening())
                    .build();
        }
    }
