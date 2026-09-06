package civ.net.server.handler;

import civ.model.map.MapCatalog;
import civ.model.map.MapPreset;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.request.SelectMapRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SelectMapHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        SelectMapRequest request = (SelectMapRequest) message;
        Lobby lobby = session.getLobby();

        if (!lobby.isHost(client.getClientId())) {
            client.send(new ErrorResponse(Errors.HOST_ONLY));
            return;
        }

        String mapName = request.getMapName();
        if (mapName == null || !knownMaps().contains(mapName)) {
            client.send(new ErrorResponse("Unknown map: " + mapName));
            return;
        }

        lobby.setSelectedMap(mapName);
        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(session.buildLobbyState());
    }

    private static Set<String> knownMaps() {
        Set<String> names = new HashSet<>();
        try {
            for (MapPreset preset : MapCatalog.all()) {
                names.add(preset.getResourceName());
            }
        } catch (IOException ignored) {
            names.add("crossroads.map");
            names.add("two-rivers.map");
            names.add("highlands.map");
        }
        return names;
    }
}
