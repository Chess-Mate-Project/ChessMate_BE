package backend.chessmate.global.user.service;

import backend.chessmate.global.auth.config.UserPrincipal;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.response.*;
import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.dto.response.streak.UserStreak;
import backend.chessmate.global.user.dto.response.streak.UserStreakResponse;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.entity.Streak;
import backend.chessmate.global.user.repository.StreaksRepository;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final StreaksRepository streaksRepository;
    private final RedisService redisService;
    private final LichessUtil lichessUtil;
    private final TierUtil tierUtil;


    @Value("${spring.data.redis.key.perf_key_base}")
    private String REDIS_PERF_KEY_BASE;

    @Value("${spring.data.redis.key.games_key_base}")
    private String REDIS_GAMES_KEY_BASE;

    @Value("${spring.data.redis.key.streak_key_base}")
    private String REDIS_STREAK_KEY_BASE;

    @Value("${lichess.ttl.account}")
    private long acountTTL;

    @Value("${lichess.ttl.perf}")
    private long perfTTL;

    @Value("${lichess.ttl.games}")
    private long gamesTTL;


    public Mono<UserPerfResponse> processUserPerf(GameType gameType, UserPrincipal u) {
        User user = u.getUser();
        String key = REDIS_PERF_KEY_BASE + ":" + gameType + ":" + user.getLichessId();

        return redisService.hasKeyMono(key)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("==== Redis 캐시 hit ====");
                        return redisService.getMono(key, UserPerfResponse.class);
                    } else {
                        log.info("==== Redis 캐시 miss, UserPerf API 호출 ====");
                        return lichessUtil.callUserPerfApi(user, gameType)
                                .flatMap(perf -> UserPerfResponse.from(Mono.just(perf), tierUtil))
                                .flatMap(response -> redisService.saveMono(key, response, perfTTL)
                                        .thenReturn(response));

                    }
                });
    }


    public Mono<GamesInUserInfo> processUserGamesInUserInfo(UserPrincipal u) {
        User user = u.getUser();
        String key = REDIS_GAMES_KEY_BASE + ":" + user.getLichessId();

        return redisService.hasKeyMono(key)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("==== Redis 캐시 hit ====");
                        return redisService.getMono(key, GamesInUserInfo.class);
                    } else {
                        log.info("==== Redis 캐시 miss, UserGames API 호출 ====");
                        long until = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        long since = LocalDateTime.now().minusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        return lichessUtil.callUserGamesApi(user, since, until)
                                .map(games -> {
                                    Map<String, Long> openingMap = new HashMap<>();
                                    Map<String, Long> firstMoveMap = new HashMap<>();
                                    for (UserGame game : games.getGames()) {
                                        if (game.getOpening() != null && game.getOpening().getName() != null) {
                                            String opening = game.getOpening().getName();
                                            if (opening.contains(":")) {
                                                opening = opening.split(":")[0].trim();
                                            }
                                            openingMap.merge(opening, 1L, Long::sum);
                                        }
                                        String moves = game.getMoves();
                                        if (moves != null && !moves.isEmpty()) {
                                            String firstMove = moves.split(" ")[0];
                                            firstMoveMap.merge(firstMove, 1L, Long::sum);
                                        }
                                    }
                                    log.info("openingMap 초기값: {}", openingMap);
                                    log.info("firstMoveMap 초기값: {}", firstMoveMap);

                                    String opening = openingMap.entrySet().stream()
                                            .max(Map.Entry.comparingByValue())
                                            .map(Map.Entry::getKey)
                                            .orElse("N/A");
                                    int openingCount = openingMap.getOrDefault(opening, 0L).intValue();

                                    String firstMove = firstMoveMap.entrySet().stream()
                                            .max(Map.Entry.comparingByValue())
                                            .map(Map.Entry::getKey)
                                            .orElse("N/A");
                                    int firstMoveCount = firstMoveMap.getOrDefault(firstMove, 0L).intValue();

                                    GamesInUserInfo userInfo = GamesInUserInfo.builder()
                                            .opening(opening)
                                            .openingCount(openingCount)
                                            .firstMove(firstMove)
                                            .firstMoveCount(firstMoveCount)
                                            .build();

                                    redisService.saveMono(key, userInfo, gamesTTL);
                                    return userInfo;
                                });
                    }
                });
    }

    @Transactional
    public UserStreakResponse processUserGamesInYearStreak(UserPrincipal u) {

        User user = u.getUser();

        if (streaksRepository.existsByUser(user)) {
            List<Streak> userStreaks = streaksRepository.findAllByUserOrderByDateDesc(user);

            List<UserStreak> streaks = new ArrayList<>();
            for (Streak streak : userStreaks) {
                UserStreak s = UserStreak.from(streak);
                streaks.add(s);
            }

            return UserStreakResponse.builder()
                    .streaks(streaks)
                    .build();
        }

        LocalDateTime startOfYear = LocalDateTime.of(java.time.LocalDate.now().getYear(), 1, 1, 0, 0, 0);
        long since = startOfYear.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long until = java.time.Instant.now().toEpochMilli();

        log.info("=== getUserGames 호출됨 ===");
        UserGames games = lichessUtil.callUserGamesApi(u.getUser(), since, until).block();


        Map<LocalDate, UserStreak> streakMap = new HashMap<>();

        for (UserGame game : games.getGames()) {

            LocalDate date = Instant.ofEpochMilli(game.getCreatedAt())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            UserStreak streak = streakMap.getOrDefault(date, UserStreak.builder()
                    .date(date)
                    .winCount(0)
                    .loseCount(0)
                    .drawCount(0)
                    .build());

            // 게임의 상태에 따라 승, 패, 무승부 카운트 업데이트
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
        List<UserStreak> userStreaks = new ArrayList<>(streakMap.values());

        userStreaks.sort(Comparator.comparing(UserStreak::getDate));

        for (UserStreak streak : userStreaks) {
            Streak s = Streak.builder()
                    .user(user)
                    .date(streak.getDate())
                    .played(streak.getWinCount() + streak.getLoseCount() + streak.getDrawCount() > 0)
                    .winCount(streak.getWinCount())
                    .loseCount(streak.getLoseCount())
                    .drawCount(streak.getDrawCount())
                    .build();
            streaksRepository.save(s);
        }

        return UserStreakResponse.builder()
                .streaks(userStreaks)
                .build();
    }


    public Mono<UserInfoResponse> getUserInfo(UserPrincipal u) {
        User user = u.getUser();

        return processUserGamesInUserInfo(u)
                .map(gamesInUserInfo -> UserInfoResponse.builder()
                        .userName(user.getLichessId())
                        .profile(user.getProfile())
                        .banner(user.getBanner())
                        .intro(user.getIntro())
                        .gamesInUserInfo(gamesInUserInfo)
                        .build()
                );
    }
}
