package civ.net.server;

import java.util.LinkedHashMap;
import java.util.Map;

public class Lobby {

    public static class Seat {
        public final String clientId;
        public final String name;
        public boolean ready;
        public boolean host;

        Seat(String clientId, String name) {
            this.clientId = clientId;
            this.name = name;
        }
    }

    /** LinkedHashMap keeps join order, so the list does not jump around on screen. */
    private final Map<String, Seat> seats = new LinkedHashMap<>();
    private String selectedMap = "crossroads.map";
    private boolean cheatsEnabled = false;

    public boolean nameTaken(String name) {
        return seats.values().stream().anyMatch(seat -> seat.name.equalsIgnoreCase(name));
    }

    public Seat join(String clientId, String name) {
        Seat seat = new Seat(clientId, name);
        seat.host = seats.isEmpty();
        seats.put(clientId, seat);
        return seat;
    }

    public void leave(String clientId) {
        Seat leaving = seats.remove(clientId);
        if (leaving != null && leaving.host && !seats.isEmpty()) {
            seats.values().iterator().next().host = true;
        }
    }

    public boolean isHost(String clientId) {
        Seat seat = seats.get(clientId);
        return seat != null && seat.host;
    }

    public boolean allReady() {
        return !seats.isEmpty() && seats.values().stream().allMatch(seat -> seat.ready);
    }

    public String nameOf(String clientId) {
        Seat seat = seats.get(clientId);
        return seat == null ? "Unknown" : seat.name;
    }

    public Seat getSeat(String clientId) {
        return seats.get(clientId);
    }

    public Map<String, Seat> getSeats() {
        return seats;
    }

    public String getSelectedMap() {
        return selectedMap;
    }

    public void setSelectedMap(String map) {
        this.selectedMap = map;
    }

    public boolean isCheatsEnabled() {
        return cheatsEnabled;
    }

    public void setCheatsEnabled(boolean on) {
        this.cheatsEnabled = on;
    }
}
