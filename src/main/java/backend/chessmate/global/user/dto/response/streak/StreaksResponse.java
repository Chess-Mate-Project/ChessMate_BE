package backend.chessmate.global.user.dto.response.streak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class StreaksResponse {
    private List<Streak> streaks;
}
