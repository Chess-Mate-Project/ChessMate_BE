package backend.chessmate.global.user.dto.response;

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
