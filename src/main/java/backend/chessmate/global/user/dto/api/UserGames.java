package backend.chessmate.global.user.dto.api;

import lombok.Data;

import java.util.List;

@Data
public class UserGames {
    private List<UserGame> games;
}
