package backend.chessmate.global.user.utils;

import backend.chessmate.global.user.dto.api.UserGame;
import org.springframework.stereotype.Component;

@Component
public class UserGamesUtil {


    public String getUserColor(UserGame game, String userName) {
        if (game.getPlayers() == null) return null;

        if (game.getPlayers().getWhite() != null &&
                game.getPlayers().getWhite().getUser() != null &&
                userName.equalsIgnoreCase(game.getPlayers().getWhite().getUser().getName())) {
            return "white";
        }

        if (game.getPlayers().getBlack() != null &&
                game.getPlayers().getBlack().getUser() != null &&
                userName.equalsIgnoreCase(game.getPlayers().getBlack().getUser().getName())) {
            return "black";
        }

        return null;
    }
}
