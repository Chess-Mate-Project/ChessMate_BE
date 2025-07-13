package backend.chessmate.global.user.utils;

import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.global.user.dto.api.UserAccount;
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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LichessUtil {

    private final RedisService redisService;

    @Value("${lichess.client-id}")
    private String clientId;

    @Value("${lichess.redirect-url}")
    private String redirectUrl;

    @Value("${lichess.base-url}")
    private String baseUrl;

    @Value("${lichess.ttl.account}")
    private long acctountTTL;

    @Value("${spring.data.redis.key.games_key_base}")
    private String REDIS_GAMES_KEY;


    public LichessUtil(RedisService redisService) {
        this.redisService = redisService;
    }

    public Mono<OAuthAccessTokenResponse> getOAuthAccessToken(OAuthValueRequest request) {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            return webClient.post()
                    .uri("/api/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("grant_type", "authorization_code")
                            .with("code", request.getCode())
                            .with("client_id", clientId)
                            .with("redirect_uri", redirectUrl)
                            .with("code_verifier", request.getCodeVerifier()))
                    .retrieve()
                    .bodyToMono(OAuthAccessTokenResponse.class)
                    .doOnError(e -> {
                        throw new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN);
                    });
    }



    public Mono<UserAccount> getUserAccount(String token) {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            return webClient.get()
                    .uri("/api/account")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(UserAccount.class)
                    .doOnError(e -> {
                        throw new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
                    });

    }

    public Mono<UserPerf> callUserPerfApi(User u, GameType gameType) {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();


            return webClient.get()
                    .uri("/api/user/{userName}/perf/{gameType}", u.getName(), gameType)
                    .retrieve()
                    .bodyToMono(UserPerf.class)
                    .doOnError(e -> {
                        throw new UserException(UserErrorCode.FAILD_GET_USER_PERF);
                    });

    }


    public Mono<UserGames> callUserGamesApi(User u,long since, long until) {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Accept", "application/json")
                    .build();

            log.info(baseUrl + "/api/games/user/AVDNA8?opening=true&since=" + since + "&until=" + until);

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/games/user/{userName}")
                            .queryParam("opening", "true")
                            .queryParam("since", since)
                            .queryParam("until", until)
                            .build("Pap-G"))
                    .accept(MediaType.valueOf("application/x-ndjson"))
                    .retrieve()
                    .bodyToFlux(UserGame.class) // NDJSON은 스트림이니까 Flux로 받음
                    .collectList()
                    .map(list -> {
                        UserGames games = new UserGames();
                        games.setGames(list);
                        return games;
                    })
                    .doOnError(e -> {
                        throw new UserException(UserErrorCode.FAILD_GET_USER_GAMES);
                    });
    }
}

