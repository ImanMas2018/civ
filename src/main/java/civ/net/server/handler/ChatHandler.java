package civ.net.server.handler;

import civ.net.protocol.Message;
import civ.net.protocol.request.ChatRequest;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;

public class ChatHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        ChatRequest request = (ChatRequest) message;
        session.publishChat(session.nameOf(client), request.getText());
    }
}
