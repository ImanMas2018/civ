package civ.net.protocol.push;

import civ.net.protocol.Broadcast;
import civ.net.protocol.dto.GameStateDto;

/** Placeholder for Step 3 — registered so the codec already knows the type. */
public class GameStateBroadcast extends Broadcast {

    public static final String TYPE = "game_state";

    private final GameStateDto state;

    public GameStateBroadcast(GameStateDto state) {
        super(TYPE);
        this.state = state;
    }

    public GameStateDto getState() {
        return state;
    }
}
