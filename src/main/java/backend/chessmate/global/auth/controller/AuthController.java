package backend.chessmate.global.auth.controller;

import backend.chessmate.global.auth.dto.OAuthValueRequest;
import backend.chessmate.global.auth.service.AuthService;
import backend.chessmate.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SuccessResponse<?>> login(@RequestBody OAuthValueRequest request) {

        authService.login(request);
        return ResponseEntity.ok(
            new SuccessResponse<>("로그인 성공", null)
        );
    }
}
