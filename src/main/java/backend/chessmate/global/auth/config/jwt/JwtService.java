package backend.chessmate.global.auth.config.jwt;

import backend.chessmate.global.auth.config.CustomUserDetailsService;
import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.Duration;
import static backend.chessmate.global.auth.config.jwt.JwtRule.*;

@Service
public class JwtService {
    private final CustomUserDetailsService userDetailsService;
    private final JwtGenerator generator;
    private final JwtUtil util;
    private final StringRedisTemplate redis;

    private final Key ACCESS_KEY;
    private final Key REFRESH_KEY;
    private final long ACCESS_EXP;
    private final long REFRESH_EXP;

    // 생성자: 의존성 및 JWT 관련 설정값 주입
    public JwtService(
            CustomUserDetailsService userDetailsService,
            JwtGenerator jwtGenerator,
            JwtUtil jwtUtil,
            StringRedisTemplate redisTemplate,
            @Value("${spring.jwt.access-token.secret}") String accessSecret,
            @Value("${spring.jwt.refresh-token.secret}") String refreshSecret,
            @Value("${spring.jwt.access-token.expiration}") long accessExpiration,
            @Value("${spring.jwt.refresh-token.expiration}") long refreshExpiration
    ) {
        this.userDetailsService = userDetailsService;
        this.generator = jwtGenerator;
        this.util = jwtUtil;
        this.redis = redisTemplate;
        this.ACCESS_KEY = jwtUtil.getSigningKey(accessSecret);
        this.REFRESH_KEY = jwtUtil.getSigningKey(refreshSecret);
        this.ACCESS_EXP = accessExpiration;
        this.REFRESH_EXP = refreshExpiration;
    }

    // 1) Access Token 발급
    @Transactional
    public String generateAccessToken(HttpServletResponse res, User u) {
        String at = generator.generateAccessToken(ACCESS_KEY, ACCESS_EXP, u);
        ResponseCookie cookie = ResponseCookie.from(ACCESS_PREFIX.getValue(), at)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(ACCESS_EXP / 1000)
                .build();
        res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());
        return at;
    }

    // 2) Refresh Token 발급 + Redis 저장(RTR)
    @Transactional
    public String generateRefreshToken(HttpServletResponse res, User u) {
        String rt = generator.generateRefreshToken(REFRESH_KEY, REFRESH_EXP, u);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_PREFIX.getValue(), rt)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(REFRESH_EXP / 1000)
                .build();
        res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());

        // Redis에 저장: REFRESH:{lichessId}
        String key = "REFRESH:" + u.getLichessId();
        redis.opsForValue().set(key, rt, Duration.ofMillis(REFRESH_EXP));

        return rt;
    }

    // 3) Access Token 검증
    public boolean validateAccessToken(String t) {
        return util.getTokenStatus(t, ACCESS_KEY) == TokenStatus.AUTHENTICATED;
    }

    // 4) Refresh Token 검증 (서명 + Redis 일치 여부)
    public boolean validateRefreshToken(String t, String identifier) {
        boolean ok = util.getTokenStatus(t, REFRESH_KEY) == TokenStatus.AUTHENTICATED;
        if (!ok) return false;

        String key = "REFRESH:" + identifier;
        String stored = redis.opsForValue().get(key);
        return t.equals(stored);
    }

    // 5) Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        String userId = Jwts.parserBuilder()
                .setSigningKey(ACCESS_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        UserDetails principal = userDetailsService.loadUserByUsername(userId);
        return new UsernamePasswordAuthenticationToken(
                principal, "", principal.getAuthorities());
    }

    // 6) 쿠키에서 토큰 꺼내기
    public String resolveToken(HttpServletRequest req, JwtRule p) {
        jakarta.servlet.http.Cookie[] cs = req.getCookies();
        if (cs == null) throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);
        return util.resolveTokenFromCookie(cs, p);
    }

    // 7) 로그아웃 처리: Redis 키 삭제 + 쿠키 만료
    @Transactional
    public void logout(User u, HttpServletResponse res) {
        String key = "REFRESH:" + u.getLichessId();
        redis.delete(key);

        res.addCookie(util.resetToken(ACCESS_PREFIX));
        res.addCookie(util.resetToken(REFRESH_PREFIX));
    }
}
