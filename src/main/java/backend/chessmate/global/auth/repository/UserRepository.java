package backend.chessmate.global.auth.repository;

import backend.chessmate.global.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Method to find a user by email
    User findByEmail(String email);

    // Method to check if a user exists by email
    boolean existsByEmail(String email);
}
