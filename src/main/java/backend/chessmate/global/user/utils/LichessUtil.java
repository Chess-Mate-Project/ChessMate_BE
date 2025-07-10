package backend.chessmate.global.user.utils;

import backend.chessmate.global.auth.dto.response.UserAccountResponse;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.entity.GameType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LichessUtil {

    private final RedisService redisService;


    @Value("${lichess.base-url}")
    private String baseUrl;

    @Value("${lichess.ttl.account}")
    private long acctountTTL;

    @Value("${spring.data.redis.key.account_key}")
    private String REDIS_ACCOUNT_KEY;

    @Value("${spring.data.redis.key.perf_key}")
    private String REDIS_PERF_KEY;

    @Value("${spring.data.redis.key.games_key}")
    private String REDIS_GAMES_KEY;
//
//    @Value("${spring.data.redis.key.oauth_key}")
//    private String REDIS_OAUTH_KEY;


    public LichessUtil(RedisService redisService, @Value("${spring.data.redis.key.account_key}") String redisAccountKey) {
        this.redisService = redisService;
        REDIS_ACCOUNT_KEY = redisAccountKey;
    }


//    public UserEmailResponse getUserEmail(String token) {
//        WebClient webClient = WebClient.builder()
//                .baseUrl(baseUrl)
//                .build();
//
//        return webClient.get()
//                .uri("/api/account/email")
//                .headers(headers -> headers.setBearerAuth(token))
//                .retrieve()
//                .bodyToMono(UserEmailResponse.class)
//                .block(); // 동기 방식 수정 필요함
//    }


    public UserAccountResponse getUserAccount(String token) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();


            UserAccountResponse response = webClient.get()
                    .uri("/api/account")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block(); // 동기 방식 수정 필요함

            redisService.save(REDIS_ACCOUNT_KEY + response.getId(), response, acctountTTL);
            return response;
        } catch (Exception e) {
            log.error("UserAccount를 받아오는 과정에서 생긴 오류: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT);
        }
    }

    public UserPerf callUserPerfApi(User u, GameType gameType) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();


            UserPerf perf = webClient.get()
                    .uri("/api/user/{userName}/perf/{gameType}", u.getName(), gameType)
                    .retrieve()
                    .bodyToMono(UserPerf.class)
                    .block(); // 동기 방식 수정 필요함

            redisService.save(REDIS_PERF_KEY + gameType + u.getLichessId(), perf, acctountTTL);

            return perf;
        } catch (Exception e) {
            log.error("UserPerf를 받아오는 과정에서 생긴 오류: {}", e.getMessage());
            throw new UserException(AuthErrorCode.FAILD_GET_USER_ACCOUNT);
        }
    }


    public Mono<UserGames> callUserGamesApi(User u) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Accept", "application/json")

                    .build();

            LocalDateTime startOfYear = LocalDateTime.of(java.time.LocalDate.now().getYear(), 1, 1, 0, 0, 0);
            long since = startOfYear.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long until = java.time.Instant.now().toEpochMilli();

            log.info("UserGames API 호출: userName={}, since={}, until={}", "junghook", since, until);


            log.info("Lichess API 호출 경로: /api/games/user/{}?opening=true&since={}&until={}", "Sankalp_Gupta", since, until);

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/games/user/{userName}")
                            .queryParam("opening", "true")
                            .queryParam("since", since)
                            .queryParam("until", until)
                            .build("Sankalp_Gupta"))
                    .accept(MediaType.valueOf("application/x-ndjson"))
                    .retrieve()
                    .bodyToFlux(UserGame.class) // NDJSON은 스트림이니까 Flux로 받음
                    .collectList()
                    .map(list -> {
                        UserGames games = new UserGames();
                        games.setGames(list);
                        redisService.save(REDIS_GAMES_KEY + u.getLichessId(), games, acctountTTL);
                        return games;
                    });
        } catch (Exception e) {
            log.error("UserGames를 받아오는 과정에서 생긴 오류: {}", e.getMessage());
            throw new UserException(UserErrorCode.FAILD_GET_USER_GAMES);
        }
    }
}

