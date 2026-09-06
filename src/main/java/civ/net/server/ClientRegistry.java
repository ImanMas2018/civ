package civ.net.server;

import civ.net.protocol.Message;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {

    private final Map<String, ClientHandler> byClientId = new ConcurrentHashMap<>();

    public void add(ClientHandler handler) {
        byClientId.put(handler.getClientId(), handler);
    }

    public void remove(ClientHandler handler) {
        byClientId.remove(handler.getClientId());
    }

    public ClientHandler byClientId(String clientId) {
        return byClientId.get(clientId);
    }

    public Collection<ClientHandler> all() {
        return byClientId.values();
    }

    public ClientHandler byPlayerId(long playerId) {
        for (ClientHandler handler : byClientId.values()) {
            if (handler.getPlayerId() == playerId) {
                return handler;
            }
        }
        return null;
    }

    public void broadcast(Message message) {
        for (ClientHandler handler : byClientId.values()) {
            handler.send(message);
        }
    }

    public void closeAll() {
        for (ClientHandler handler : byClientId.values()) {
            handler.close();
        }
    }
}
