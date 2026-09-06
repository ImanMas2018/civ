package civ.net.server.handler;

import civ.net.protocol.Message;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;

public interface RequestHandler {

    /** Always called with the session lock already held. */
    void handle(ServerSession session, ClientHandler client, Message message);
}
