package backend.chessmate.global.user.entity;

import backend.chessmate.global.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "streak",
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_user_date", columnNames = {"lichess_id", "date"})
        })
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // User 참조
    @JoinColumn(name = "lichess_id", nullable = false)
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "played", nullable = false)
    private Boolean played = false;

    @Column(name = "win_count", nullable = false)
    private int winCount = 0;

    @Column(name = "lose_count", nullable = false)
    private int loseCount = 0;

    @Column(name = "draw_count", nullable = false)
    private int drawCount = 0;
}