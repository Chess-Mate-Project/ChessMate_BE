package backend.chessmate.global.user.trigger;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.redis.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.dto.response.GamesInUserInfo;
import backend.chessmate.global.user.dto.response.UserPerfResponse;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.entity.Streak;
import backend.chessmate.global.user.repository.StreaksRepository;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import backend.chessmate.global.user.utils.UserGamesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
public class UserScheduler {

    @Value("${spring.data.redis.key.perf_key_base}")
    private String REDIS_PERF_KEY_BASE;

    @Value("${spring.data.redis.key.games_key_base}")
    private String REDIS_GAMES_KEY_BASE;

    @Value("${lichess.ttl.perf}")
    private long perfTTL;

    @Value("${lichess.ttl.games}")
    private long gamesTTL;

    private final UserRepository userRepository;
    private final LichessUtil lichessUtil;
    private final StreaksRepository streaksRepository;
    private final UserGamesUtil userGamesUtil;
    private final RedisService redisService;
    private final TierUtil tierUtil;

    public UserScheduler(UserRepository userRepository, LichessUtil lichessUtil, StreaksRepository streaksRepository, UserGamesUtil userGamesUtil, RedisService redisService, TierUtil tierUtil) {
        this.userRepository = userRepository;
        this.lichessUtil = lichessUtil;
        this.streaksRepository = streaksRepository;
        this.userGamesUtil = userGamesUtil;
        this.redisService = redisService;
        this.tierUtil = tierUtil;
    }



    //유저의 스트릭을 매일 1시에 업데이트
    //todo : 비동기 처리가 필요함.
    @Scheduled(cron = "0 0 1 * * *")
    public void updateUserStreak() {
        LocalDate targetDate = LocalDate.now().minusDays(1); // 어제 날짜 기준으로만 수집
        long since = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long until = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<User> users = userRepository.findAll();

        for (User user : users) {
            UserGames games = lichessUtil.callUserGamesApi(user, since, until);
            if (games == null || games.getGames() == null) continue;

            int winCount = 0, loseCount = 0, drawCount = 0;

            for (UserGame game : games.getGames()) {
                if (game.getStatus().equals("draw")) {
                    drawCount++;
                    continue;
                }

                if (!"resign".equals(game.getStatus())) continue;

                String userColor = userGamesUtil.getUserColor(game, user.getName());
                String winnerColor = game.getWinner();

                if (winnerColor == null || userColor == null) continue;

                if (winnerColor.equals(userColor)) {
                    winCount++;
                } else {
                    loseCount++;
                }
            }

            boolean played = (winCount + loseCount + drawCount) > 0;

            Streak existing = streaksRepository.findByUserAndDate(user, targetDate);
            if (existing == null) {
                streaksRepository.save(Streak.builder()
                        .user(user)
                        .date(targetDate)
                        .played(played)
                        .winCount(winCount)
                        .loseCount(loseCount)
                        .drawCount(drawCount)
                        .build());
            } else {
                existing.setPlayed(played);
                existing.setWinCount(winCount);
                existing.setLoseCount(loseCount);
                existing.setDrawCount(drawCount);
                streaksRepository.save(existing);
            }
        }
    }

    //1시간을 간격으로 사용자의 각 게임 타입별 퍼포먼스를 레디스에 업데이트
    //todo : 비동기 처리 필요
    @Scheduled(cron = "0 0 * * * *")
    public void updateUserPerf() {

        List<User> users = userRepository.findAll();
        List<GameType> gameTypes = Arrays.asList(GameType.BULLET, GameType.BLITZ, GameType.RAPID, GameType.CLASSICAL);
        for (User user : users) {
            for (int i = 0 ; i < 4 ; i++) {
                GameType gameType = gameTypes.get(i);
                log.info("Perf 정보 업데이트 | 유저이름 - {} | 게임 타입 - {}", user.getName(), gameType);
                String key = REDIS_PERF_KEY_BASE + ":" + gameType + ":" + user.getLichessId();

                UserPerf perf = lichessUtil.callUserPerfApi(user, gameType);
                UserPerfResponse response = UserPerfResponse.from(perf, tierUtil);

                redisService.save(key, response, perfTTL);
            }

        }
    }

    //1시간을 간격으로 사용자의 한달 치 게임 정보를 레디스에 업데이트 (사용자 선호 첫 수 - 사용 횟수, 사용자 선호 오프닝 - 사용 횟수)
    //todo : 비동기 처리 필요
    public void updateUserGamesInfo() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            log.info("Games 정보 업데이트 | 유저이름 - {}", user.getName());
            String key = REDIS_GAMES_KEY_BASE + ":" + user.getLichessId();

            long until = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long since = LocalDateTime.now().minusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            UserGames games = lichessUtil.callUserGamesApi(user, since, until);


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

            redisService.save(key, userInfo, gamesTTL);

        }
    }



}
