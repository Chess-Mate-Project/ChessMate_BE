package backend.chessmate.global.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {

    @Id
    @Column(name = "lichess_id", nullable = false, unique = true)
    private String lichessId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "intro")
    private String intro;

    @Column(name = "profile", nullable = false)
    private int profile;

    @Column(name = "banner", nullable = false)
    private int banner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @PrePersist //새롭게 알게된 개념
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.profile = 0; // 기본 프로필 이미지
        this.banner = 0; // 기본 배너 이미지
        this.role = Role.USER; // 기본 역할은 USER로 설정
    }


}
