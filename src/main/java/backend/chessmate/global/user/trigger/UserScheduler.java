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
import backend.chessmate.global.user.service.CacheService;
import backend.chessmate.global.user.utils.LichessUtil;
import backend.chessmate.global.user.utils.TierUtil;
import backend.chessmate.global.user.utils.UserGamesUtil;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
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
    private final CacheService cacheService;


    //유저의 스트릭을 매일 1시에 업데이트
    @Scheduled(cron = "0 0 1 * * *")
    public void updateUserStreak() {

        List<User> users = userRepository.findAll();
        for (User user : users) {
            cacheService.userGamesCache(user);
        }
    }

    //1시간을 간격으로 사용자의 각 게임 타입별 퍼포먼스를 레디스에 업데이트
    @Scheduled(cron = "0 0 * * * *")
    public void updateUserPerf() {

        List<User> users = userRepository.findAll();
        for (User user : users) {
            cacheService.userPerfCache(user);
        }
    }

    //1시간을 간격으로 사용자의 한달 치 게임 정보를 레디스에 업데이트 (사용자 선호 첫 수 - 사용 횟수, 사용자 선호 오프닝 - 사용 횟수)
    @Scheduled(cron = "0 0 * * * *")
    public void updateUserGamesInfo() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            cacheService.userGamesInfo(user);
        }
    }



}
