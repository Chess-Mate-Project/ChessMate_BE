package backend.chessmate.global.auth.service;

import backend.chessmate.global.auth.dto.OAuthAccessTokenResponse;
import backend.chessmate.global.auth.dto.OAuthValueRequest;
import backend.chessmate.global.auth.dto.UserAccountResponse;
import backend.chessmate.global.auth.dto.UserEmailResponse;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${lichess.base-url}")
    private String baseUrl;

    @Value("${lichess.client-id}")
    private String clientId;

    @Value("${lichess.redirect-url}")
    private String redirectUrl;


    public void login(OAuthValueRequest request) {
        OAuthAccessTokenResponse oauthToken = getOAuthAccessToken(request);


        UserAccountResponse userAccount = getUserAccount(oauthToken.getAccessToken());
        redisService.save(userAccount.getId(), oauthToken.getAccessToken(), oauthToken.getExpiresIn());


        String userAccountJson = objectMapper.writeValueAsString(userAccount);
        redisService.save(userAccount.getId(), userAccountJson, );

        UserEmailResponse userEmail = getUserEmail(oauthToken.getAccessToken());

        User user = User.builder()
                .lichessId(userAccount.getId())
                .name(userAccount.getUsername())
                .build();

        userRepository.save(user);





    }

    public OAuthAccessTokenResponse getOAuthAccessToken(OAuthValueRequest request) {

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
    }

    public UserEmailResponse getUserEmail(String token) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        return webClient.get()
                .uri("/api/account/email")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserEmailResponse.class)
                .block(); // 동기 방식 수정 필요함
    }

    public UserAccountResponse getUserAccount(String token) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        return webClient.get()
                .uri("/api/account")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserAccountResponse.class)
                .block(); // 동기 방식 수정 필요함
    }
}
