package civ.net.protocol.push;

import civ.net.protocol.Broadcast;

/** Placeholder for Step 5. */
public class GameOverBroadcast extends Broadcast {

    public static final String TYPE = "game_over";

    private final String winnerName;

    public GameOverBroadcast(String winnerName) {
        super(TYPE);
        this.winnerName = winnerName;
    }

    public String getWinnerName() {
        return winnerName;
    }
}
