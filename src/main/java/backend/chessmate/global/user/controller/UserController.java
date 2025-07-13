package backend.chessmate.global.user.controller;

import backend.chessmate.global.auth.config.UserPrincipal;
import backend.chessmate.global.common.response.SuccessResponse;
import backend.chessmate.global.user.dto.response.streak.UserStreakResponse;
import backend.chessmate.global.user.dto.response.UserInfoResponse;
import backend.chessmate.global.user.dto.response.UserPerfResponse;
import backend.chessmate.global.user.entity.GameType;
import backend.chessmate.global.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

//    @GetMapping("/tier")
//    public ResponseEntity<SuccessResponse<TierResponse>> getUserTier(@RequestParam GameType gameType, @AuthenticationPrincipal UserPrincipal u) {
//
//        TierResponse response = userService.processUserAccount(gameType, u);
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 티어 조회 성공/ 타입 = " + gameType, response)
//        );
//
//    }

    @GetMapping("/perf")
    public ResponseEntity<SuccessResponse<UserPerfResponse>> getUserPerf(@RequestParam GameType gameType, @AuthenticationPrincipal UserPrincipal u) {

        UserPerfResponse response = userService.processUserPerf(gameType, u);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 퍼포먼스 조회 성공/ 타입 = " + gameType, response)
        );

    }

    @GetMapping("/info")
    public ResponseEntity<SuccessResponse<UserInfoResponse>> getUserInfo(@AuthenticationPrincipal UserPrincipal u) {

        UserInfoResponse response = userService.getUserInfo(u);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 정보 조회 성공", response)
        );
    }
    @GetMapping("/streaks")
    public ResponseEntity<SuccessResponse<UserStreakResponse>> getUserStreaks(@AuthenticationPrincipal UserPrincipal u) {

        UserStreakResponse response = userService.processUserGamesInYearStreak(u);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 스트릭 조회 성공" , response)
        );
    }





}
