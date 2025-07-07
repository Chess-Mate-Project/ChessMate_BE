package backend.chessmate.global.auth.repository;

import backend.chessmate.global.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByLichessId(String lichessId);
}
