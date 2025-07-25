package backend.chessmate.global.user.utils;

import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.global.user.dto.api.UserAccount;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.redis.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.api.UserPerf;
import backend.chessmate.global.user.entity.GameType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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


    public LichessUtil(RedisService redisService) {
        this.redisService = redisService;
    }

    public OAuthAccessTokenResponse getOAuthAccessToken(OAuthValueRequest request) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        String requestUri = baseUrl + "/api/token"
                + "?grant_type=authorization_code"
                + "&code=" + request.getCode()
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUrl
                + "&code_verifier=" + request.getCodeVerifier();
        log.info("OAuth 토큰 요청 경로: {}", requestUri);

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

                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.error("❌ 4xx 오류 응답 바디: {}", errorBody))
                            .then(Mono.error(new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN)));
                })


                .bodyToMono(OAuthAccessTokenResponse.class)
                .block();
    }


    public UserAccount getUserAccount(String token) {

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
                }).block();

    }


//    public UserPerf callUserPerfApi(User u, GameType gameType) {
//
//        WebClient webClient = WebClient.builder()
//                .baseUrl(baseUrl)
//                .build();
//
//
//        return webClient.get()
//                .uri("/api/user/{userName}/perf/{gameType}","Pap-G", gameType)//일단 통계가 많은 유저로 테스트
//                .retrieve()
//                .bodyToMono(UserPerf.class)
//                .doOnError(e -> {
//                    throw new UserException(UserErrorCode.FAILD_GET_USER_PERF);
//                }).block();
//
//    }
//
//
//    public UserGames callUserGamesApi(User u, long since, long until) {
//
//        WebClient webClient = WebClient.builder()
//                .baseUrl(baseUrl)
//                .defaultHeader("Accept", "application/json")
//                .build();
//
//        log.info(baseUrl + "/api/games/user/AVDNA8?opening=true&since=" + since + "&until=" + until);
//
//        return webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/games/user/{userName}")
//                        .queryParam("opening", "true")
//                        .queryParam("since", since)
//                        .queryParam("until", until)
//                        .build("Pap-G")) //일단 통계가 많은 유저로 테스트
//                .accept(MediaType.valueOf("application/x-ndjson"))
//                .retrieve()
//                .bodyToFlux(UserGame.class) // NDJSON은 스트림이니까 Flux로 받음
//                .collectList()
//                .map(list -> {
//                    UserGames games = new UserGames();
//                    games.setGames(list);
//                    return games;
//                })
//                .doOnError(e -> {
//                    throw new UserException(UserErrorCode.FAILD_GET_USER_GAMES);
//                }).block();
//    }

    public UserPerf callUserPerfApi(User u, GameType gameType) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        return webClient.get()
                .uri("/api/user/{userName}/perf/{gameType}", "Pap-G", gameType)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("❌ [4xx] UserPerf API 오류: {}", body);
                            return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_PERF));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("❌ [5xx] UserPerf API 서버 오류: {}", body);
                            return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_PERF));
                        })
                )
                .bodyToMono(UserPerf.class)
                .onErrorResume(e -> {
                    log.error("❌ [Exception] UserPerf API 호출 중 예외 발생", e);
                    return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_PERF));
                })
                .block();
    }

    public UserGames callUserGamesApi(User u, long since, long until) {
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
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("❌ [4xx] UserGames API 오류: {}", body);
                            return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_GAMES));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("❌ [5xx] UserGames API 서버 오류: {}", body);
                            return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_GAMES));
                        })
                )
                .bodyToFlux(UserGame.class)
                .collectList()
                .map(list -> {
                    UserGames games = new UserGames();
                    games.setGames(list);
                    return games;
                })
                .onErrorResume(e -> {
                    log.error("❌ [Exception] UserGames API 호출 중 예외 발생", e);
                    return Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_GAMES));
                })
                .block();
    }

}

