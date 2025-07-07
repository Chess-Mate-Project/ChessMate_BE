package backend.chessmate.global.auth.config.jwt;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();//요청 경로 추출하기

        if(uri.startsWith("/api/auth/login")) { //해당 경로 요청이면 필터 생략
            chain.doFilter(request, response);
            return;
        }


        String accessToken = jwtService.resolveToken(request, JwtRule.ACCESS_PREFIX); // 쿠키에서 엑세스 토큰 추출


        if (jwtService.validateAccessToken(accessToken)) { //엑세스 토큰 검증
            SecurityContextHolder.getContext().setAuthentication( // 엑세스 토큰으로 인증 객체 설정
                    jwtService.getAuthentication(accessToken)
            );

            chain.doFilter(request, response);
            return;
        }

        String refreshToken = jwtService.resolveToken(request, JwtRule.REFRESH_PREFIX); // 쿠키에서 리프레시 토큰 추출
        String lichessId = jwtService.getAuthentication(refreshToken).getName(); //리프레시 토큰으로 인증 객체에서 lichessId 추출 (getName()이 lichessId를 반환함)
        User user = userRepository.findByLichessId(lichessId); //lichessId로 유저 정보 조회


        if (jwtService.validateAccessToken(refreshToken)) { //리프레시 토큰 검증
            String newAccessToken = jwtService.generateAccessToken(response, user);// 새로운 엑세스 토큰 발급

            jwtService.generateRefreshToken(response, user); //새로운 리프레쉬 토큰 발급(발급 시 자동 레디스 저장) - RTR(Rotate-Refresh-Token) 이라고 함

            SecurityContextHolder.getContext().setAuthentication(
                    jwtService.getAuthentication(newAccessToken)
            );// 새로운 엑세스 토큰으로 인증 객체 설정

            chain.doFilter(request, response); //다음 필터로 이동
            return;
        }




        jwtService.logout(user, response);
        chain.doFilter(request, response);



    }
}

