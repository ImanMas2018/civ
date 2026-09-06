package civ.net.protocol.push;

import civ.net.protocol.Broadcast;
import java.util.ArrayList;
import java.util.List;

public class LobbyStateBroadcast extends Broadcast {

    public static final String TYPE = "lobby_state";

    private final List<SeatDto> seats;
    private final String selectedMap;
    private final boolean cheatsEnabled;

    public LobbyStateBroadcast(List<SeatDto> seats, String selectedMap, boolean cheatsEnabled) {
        super(TYPE);
        this.seats = seats == null ? new ArrayList<>() : new ArrayList<>(seats);
        this.selectedMap = selectedMap;
        this.cheatsEnabled = cheatsEnabled;
    }

    public List<SeatDto> getSeats() {
        return seats;
    }

    public String getSelectedMap() {
        return selectedMap;
    }

    public boolean isCheatsEnabled() {
        return cheatsEnabled;
    }

    public static class SeatDto {
        public String name;
        public boolean ready;
        public boolean host;

        public SeatDto() {
        }

        public SeatDto(String name, boolean ready, boolean host) {
            this.name = name;
            this.ready = ready;
            this.host = host;
        }
    }
}
