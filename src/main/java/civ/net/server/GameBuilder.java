package civ.net.server;

import civ.model.Game;
import civ.model.map.MapPreset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Builds the authoritative Game from lobby seats and the chosen map. */
public final class GameBuilder {

    private GameBuilder() {
    }

    public static Game build(Lobby lobby) throws IOException {
        MapPreset preset = new MapPreset(lobby.getSelectedMap());
        List<String> names = new ArrayList<>();
        for (Lobby.Seat seat : lobby.getSeats().values()) {
            names.add(seat.name);
        }
        return new Game(System.currentTimeMillis(), names, preset);
    }
}
