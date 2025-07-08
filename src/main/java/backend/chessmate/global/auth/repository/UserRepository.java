package backend.chessmate.global.auth.repository;

import backend.chessmate.global.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByLichessId(String lichessId);
}
