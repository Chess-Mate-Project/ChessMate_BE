package backend.chessmate.global.auth.config.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class JwtUtil {
    // 1) 토큰 상태 확인
    public TokenStatus getTokenStatus(String token, Key key) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            log.info("JWT 토큰 인증 성공: {}", token);
            return TokenStatus.AUTHENTICATED;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: {}", token, e);
            return TokenStatus.EXPIRED;
        } catch (JwtException e) {
            log.error("JWT 토큰 무효: {}", token, e);
            return TokenStatus.INVALID;
        }
    }

    // 2) 쿠키에서 토큰 값만 뽑기
    public String resolveTokenFromCookie(Cookie[] cookies, JwtRule prefix) {
        if (cookies == null) return "";

        String targetCookieName = prefix.getValue();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(targetCookieName)) {
                return cookie.getValue();
            }
        }

        return "";
    }

    // 3) 시크릿 문자열 → Key
    public Key getSigningKey(String baseSecret) {
        String encoded = Base64.getEncoder()
                .encodeToString(baseSecret.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(encoded.getBytes(StandardCharsets.UTF_8));
    }

    // 4) 쿠키 삭제용
    public Cookie resetToken(JwtRule prefix) {
        Cookie c = new Cookie(prefix.getValue(), null);
        c.setPath("/");
        c.setMaxAge(0);  // 즉시 만료
        return c;
    }
}
