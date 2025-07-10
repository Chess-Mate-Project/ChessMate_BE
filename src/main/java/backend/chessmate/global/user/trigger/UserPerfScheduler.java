package backend.chessmate.global.user.trigger;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.auth.repository.UserRepository;
import backend.chessmate.global.user.utils.LichessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserPerfScheduler {

    private final LichessUtil lichessUtil;
    private final UserRepository userRepository;


    public UserPerfScheduler(LichessUtil lichessUtil, UserRepository userRepository) {
        this.lichessUtil = lichessUtil;
        this.userRepository = userRepository;
    }


    @Scheduled(cron = "0 0/30 * * * *")
    public void User() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                lichessUtil.callUserGamesApi(user);
                log.info("동기처리 확인" + user.getName());
            } catch (Exception e) {
                // 예외 처리 로직 (예: 로그 기록)
                System.err.println("Error fetching user account for " + user.getLichessId() + ": " + e.getMessage());
            }
        }

    }
}
