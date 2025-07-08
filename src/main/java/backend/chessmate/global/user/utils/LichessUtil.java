package backend.chessmate.global.user.utils;

import backend.chessmate.global.auth.dto.response.UserAccountResponse;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.config.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class LichessUtil {

    private final RedisService redisService;


    @Value("${lichess.base-url}")
    private String baseUrl;

    @Value("${lichess.ttl.account}")
    private long acctountTTL;


    private final String REDIS_ACCOUNT_KEY;

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

}
