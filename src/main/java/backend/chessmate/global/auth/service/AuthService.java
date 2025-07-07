package backend.chessmate.global.auth.service;

import backend.chessmate.global.auth.config.jwt.JwtService;
import backend.chessmate.global.auth.dto.response.LoginResponse;
import backend.chessmate.global.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.dto.response.UserAccountResponse;
import backend.chessmate.global.auth.entity.Role;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.config.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${lichess.base-url}")
    private String baseUrl;

    @Value("${lichess.client-id}")
    private String clientId;

    @Value("${lichess.redirect-url}")
    private String redirectUrl;

    @Value("${lichess.ttl.account}")
    private long acctountTTL;

    private final String REDIS_KEY_PREFIX = "lichessId:";

    public LoginResponse login(OAuthValueRequest request, HttpServletResponse res) {
        OAuthAccessTokenResponse oauthToken = getOAuthAccessToken(request);

        // 유저의 OAuthAccessToken을 Redis에 저장
        UserAccountResponse userAccount = getUserAccount(oauthToken.getAccessToken());
        redisService.save(userAccount.getId(), oauthToken.getAccessToken(), oauthToken.getExpiresIn());



        redisService.save(REDIS_KEY_PREFIX + userAccount.getId(), userAccount, acctountTTL);

//        UserEmailResponse userEmail = getUserEmail(oauthToken.getAccessToken()); // 이메일이 필요한가?

        User user = User.builder()
                .lichessId(userAccount.getId())
                .name(userAccount.getUsername())
                .role(Role.USER)
                .build();
        userRepository.save(user);


        String accessToken = jwtService.generateAccessToken(res, user);
        String refreshToken = jwtService.generateRefreshToken(res, user);

        return LoginResponse.builder()
                .type("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public OAuthAccessTokenResponse getOAuthAccessToken(OAuthValueRequest request) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            log.info("Request Code: {}", request.getCode());
            log.info("Request Code Verifier: {}", request.getCodeVerifier());
            log.info("Client ID: {}", clientId);
            log.info("Redirect URL: {}", redirectUrl);



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
                    .block();// 동기 방식 수정 필요함
        } catch (Exception e) {
            log.error("OAuthAccessToken을 받아오는 과정에서 생긴 오류: {}", e.getMessage());
            if (e instanceof WebClientResponseException) {
                WebClientResponseException we = (WebClientResponseException) e;
                log.error("Response body: {}", we.getResponseBodyAsString());
            }
            throw new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN);
        }


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

            return webClient.get()
                    .uri("/api/account")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .block(); // 동기 방식 수정 필요함
        } catch (Exception e) {
            log.error("UserAccount를 받아오는 과정에서 생긴 오류: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT);
        }
    }
}
