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
import backend.chessmate.global.user.dto.response.streak.Streak;
import backend.chessmate.global.user.dto.response.streak.StreaksResponse;
import backend.chessmate.global.user.dto.response.tier.TierResult;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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



    @Value("${spring.data.redis.key.perf_key_base}")
    private String REDIS_PERF_KEY_BASE;

    @Value("${spring.data.redis.key.games_key_base}")
    private String REDIS_GAMES_KEY_BASE;

    @Value("${lichess.ttl.account}")
    private long acountTTL;

    @Value("${lichess.ttl.perf}")
    private long perfTTL;


    public UserPerfResponse processUserPerf(GameType gameType, UserPrincipal u) {
        User user = u.getUser();
        
        String key = REDIS_PERF_KEY_BASE + ":" + gameType + ":" + user.getLichessId();

        Mono<UserPerf> perf = lichessUtil.callUserPerfApi(user, gameType);
        UserPerfResponse response = UserPerfResponse.from(perf, tierUtil);

        
        if (redisService.hasKey(REDIS_PERF_KEY_BASE + ":" + gameType + ":" + user.getLichessId())) {
            log.info("==== Redis 캐시 hit ====");
            return redisService.get(key, UserPerfResponse.class);
            
        }

        log.info("==== Redis 캐시 miss ==== API 호출 ====");
        redisService.save(key, response, perfTTL);
        return response;
    }


    public GamesInUserInfo processUserGamesInUserInfo(UserPrincipal u) {
        log.info("==== getUserGames 호출됨 ====");
        User user = u.getUser();

        String key = REDIS_GAMES_KEY_BASE + user.getLichessId();
        UserGames games = null;
        if (redisService.hasKey(key)) {
            log.info("==== Redis 캐시 hit ====");
            games = redisService.get(key, UserGames.class);
        } else {
            log.info("==== Redis 캐시 miss === API 호출 ====");
            games = lichessUtil.callUserGamesApi(u.getUser()).block();
        }
        Map<String, Long> openingMap = new HashMap<>();
        Map<String, Long> firstMoveMap = new HashMap<>();
        for (UserGame game : games.getGames()) {
            if (game.getOpening() == null || game.getOpening().getName() == null) {
                continue; // opening 정보가 없으면 건너뜀
            }
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

        String key = REDIS_GAMES_KEY_BASE + user.getLichessId();
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
            String key = REDIS_GAMES_KEY_BASE + user.getLichessId();

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
