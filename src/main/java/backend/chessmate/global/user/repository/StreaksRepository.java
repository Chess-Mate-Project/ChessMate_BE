package backend.chessmate.global.user.repository;

import backend.chessmate.global.auth.entity.User;
import backend.chessmate.global.user.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreaksRepository extends JpaRepository<Streak, Long> {
    boolean existsByUser(User user);

List<Streak> findAllByUserOrderByDateDesc(User user);
}
