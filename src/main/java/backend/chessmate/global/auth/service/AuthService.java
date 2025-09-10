package backend.chessmate.global.auth.service;


import backend.chessmate.global.auth.config.jwt.JwtService;
import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.global.auth.entity.Role;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.redis.RedisService;
import backend.chessmate.global.user.dto.api.UserAccount;
import backend.chessmate.global.user.entity.BannerType;
import backend.chessmate.global.user.service.UserService;
import backend.chessmate.global.user.utils.LichessUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LichessUtil lichessutil;
    private final UserService userService;


    @Value("${spring.data.redis.key.oauth_key_base}")
    private String REDIS_OAUTH_KEY;


    public void login(OAuthValueRequest request, HttpServletResponse res) {

        OAuthAccessTokenResponse oauthTokenResponse = lichessutil.getOAuthAccessToken(request);
        String oauthToken = oauthTokenResponse.getAccessToken();


        //우선 UserAccount 호출 후 LichessId로 레디스 키 구성
        UserAccount userAccount = lichessutil.getUserAccount(oauthToken);
        String key = REDIS_OAUTH_KEY + ":" + userAccount.getId();

        // 유저의 OAuthAccessToken을 Redis에 저장
        redisService.save(key, oauthToken, oauthTokenResponse.getExpiresIn());

        Optional<User> userOptional = userRepository.findByLichessId(userAccount.getId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("이미 존재하는 사용자입니다. Lichess ID: {}", userAccount.getId());
            String accessToken = jwtService.generateAccessToken(res, user);
            String refreshToken = jwtService.generateRefreshToken(res, user);
            return;
        }

        User newUser = User.builder()
                .lichessId(userAccount.getId())
                .name(userAccount.getUsername())
                .role(Role.USER)
                .banner(BannerType.TESTBANNER)
                .build();
        userRepository.save(newUser);


        String accessToken = jwtService.generateAccessToken(res, newUser);
        String refreshToken = jwtService.generateRefreshToken(res, newUser);

    }

    public void logout(User user, HttpServletResponse res) {
        String lichessId = user.getLichessId();
        String key = REDIS_OAUTH_KEY + ":" + lichessId;

        // Redis에서 OAuth 토큰 삭제
        redisService.delete(key);

        // JWT 쿠키 삭제
        jwtService.logout(user, res);
    }


}
