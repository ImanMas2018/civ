package civ.net.server;

import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.request.SelectMapRequest;
import civ.net.protocol.request.StartGameRequest;
import civ.net.server.handler.ChatHandler;
import civ.net.server.handler.JoinHandler;
import civ.net.server.handler.ReadyHandler;
import civ.net.server.handler.RequestHandler;
import civ.net.server.handler.SelectMapHandler;
import civ.net.server.handler.StartGameHandler;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter {

    private final Map<String, RequestHandler> handlers = new HashMap<>();

    public RequestRouter(ServerSession session) {
        handlers.put(JoinRequest.TYPE, new JoinHandler());
        handlers.put(ReadyRequest.TYPE, new ReadyHandler());
        handlers.put(SelectMapRequest.TYPE, new SelectMapHandler());
        handlers.put(StartGameRequest.TYPE, new StartGameHandler());
        handlers.put(ChatRequest.TYPE, new ChatHandler());
    }

    public RequestHandler handlerFor(String type) {
        return handlers.get(type);
    }
}
