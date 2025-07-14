package backend.chessmate.global.auth.service;


import backend.chessmate.global.auth.config.jwt.JwtService;
import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.dto.response.LoginResponse;
import backend.chessmate.global.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.global.auth.entity.Role;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserAccount;
import backend.chessmate.global.user.service.UserService;
import backend.chessmate.global.user.utils.LichessUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


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


    public LoginResponse login(OAuthValueRequest request, HttpServletResponse res) {

        OAuthAccessTokenResponse oauthTokenResponse = lichessutil.getOAuthAccessToken(request);
        String oauthToken = oauthTokenResponse.getAccessToken();


        //우선 UserAccount 호출 후 LichessId로 레디스 키 구성
        UserAccount userAccount = lichessutil.getUserAccount(oauthToken);
        String key = REDIS_OAUTH_KEY + ":" + userAccount.getId();

        // 유저의 OAuthAccessToken을 Redis에 저장
        redisService.save(key, oauthToken, oauthTokenResponse.getExpiresIn());


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


}
