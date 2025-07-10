package backend.chessmate.global.user.trigger;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.config.RedisService;
import backend.chessmate.global.user.dto.api.UserGames;
import backend.chessmate.global.user.utils.LichessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserScheduler {

    private final UserRepository userRepository;
    private final LichessUtil lichessUtil;


    public UserScheduler(UserRepository userRepository, LichessUtil lichessUtil) {
        this.userRepository = userRepository;
        this.lichessUtil = lichessUtil;
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void UserYearGamesScheduler() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            log.info("UserScheduler - 유저 스트릭 정보 갱신 시작 - 유저: {}", user.getName());
            lichessUtil.callUserGamesApi(user);
            log.info("UserScheduler - 유저 스트릭 정보 갱신 완료 - 유저: {}", user.getName());
        }
    }
}
