package backend.chessmate.global.user.trigger;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserGame;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.dto.response.streak.UserStreak;
import backend.chessmate.global.user.entity.Streak;
import backend.chessmate.global.user.repository.StreaksRepository;
import backend.chessmate.global.user.utils.LichessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
public class UserScheduler {

    private final UserRepository userRepository;
    private final LichessUtil lichessUtil;
    private final StreaksRepository streaksRepository;

    public UserScheduler(UserRepository userRepository, LichessUtil lichessUtil, StreaksRepository streaksRepository) {
        this.userRepository = userRepository;
        this.lichessUtil = lichessUtil;
        this.streaksRepository = streaksRepository;
    }




    //유저의 스트릭을 매일 1시에 업데이트하여 제공
    //todo : 비동기 처리가 필요함.
    @Scheduled(cron = "0 0 1 * * *")
    public void updateUserStreak() {
        LocalDate targetDate = LocalDate.now().minusDays(1); // 어제 날짜 기준으로만 수집
        long since = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long until = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<User> users = userRepository.findAll();

        for (User user : users) {
            UserGames games = lichessUtil.callUserGamesApi(user, since, until);
            if (games == null || games.getGames() == null) continue;

            int winCount = 0, loseCount = 0, drawCount = 0;

            for (UserGame game : games.getGames()) {
                if (game.getStatus().equals("draw")) {
                    drawCount++;
                    continue;
                }

                if (!"resign".equals(game.getStatus())) continue;

                String userColor = getUserColor(game, user.getName());
                String winnerColor = game.getWinner();

                if (winnerColor == null || userColor == null) continue;

                if (winnerColor.equals(userColor)) {
                    winCount++;
                } else {
                    loseCount++;
                }
            }

            boolean played = (winCount + loseCount + drawCount) > 0;

            Streak existing = streaksRepository.findByUserAndDate(user, targetDate);
            if (existing == null) {
                streaksRepository.save(Streak.builder()
                        .user(user)
                        .date(targetDate)
                        .played(played)
                        .winCount(winCount)
                        .loseCount(loseCount)
                        .drawCount(drawCount)
                        .build());
            } else {
                existing.setPlayed(played);
                existing.setWinCount(winCount);
                existing.setLoseCount(loseCount);
                existing.setDrawCount(drawCount);
                streaksRepository.save(existing);
            }
        }
    }

    private String getUserColor(UserGame game, String userName) {
        if (game.getPlayers() == null) return null;

        if (game.getPlayers().getWhite() != null &&
                game.getPlayers().getWhite().getUser() != null &&
                userName.equalsIgnoreCase(game.getPlayers().getWhite().getUser().getName())) {
            return "white";
        }

        if (game.getPlayers().getBlack() != null &&
                game.getPlayers().getBlack().getUser() != null &&
                userName.equalsIgnoreCase(game.getPlayers().getBlack().getUser().getName())) {
            return "black";
        }

        return null;
    }

}
