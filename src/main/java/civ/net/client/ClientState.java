package civ.net.client;

import civ.net.protocol.dto.GameStateDto;
import java.util.ArrayList;
import java.util.List;

/** Throwaway copy of the last server snapshot. Replaced wholesale, never edited. */
public class ClientState {

    private GameStateDto snapshot;

    private final List<Runnable> listeners = new ArrayList<>();

    public GameStateDto get() {
        return snapshot;
    }

    public void replace(GameStateDto next) {
        this.snapshot = next;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }
}
