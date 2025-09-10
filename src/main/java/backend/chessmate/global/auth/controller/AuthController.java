package backend.chessmate.global.auth.controller;

import backend.chessmate.global.auth.config.UserPrincipal;
import backend.chessmate.global.auth.dto.request.OAuthValueRequest;
import backend.chessmate.global.auth.service.AuthService;
import backend.chessmate.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<?>> login(@RequestBody OAuthValueRequest req, HttpServletResponse res) {

       authService.login(req, res);
        return ResponseEntity.ok(
            new SuccessResponse<>("로그인 성공", null)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Object>> logout(@AuthenticationPrincipal UserPrincipal u, HttpServletResponse res) {
        authService.logout(u.getUser(), res);
        return ResponseEntity.ok(
            new SuccessResponse<>("로그아웃 성공", null)
        );
    }
}
