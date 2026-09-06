package civ.net.server.handler;

import civ.net.protocol.Message;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.request.ChatRequest;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatHandler implements RequestHandler {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_LENGTH = 500;

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        ChatRequest request = (ChatRequest) message;

        String text = request.getText() == null ? "" : request.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }

        String sender = session.nameOf(client);
        ChatBroadcast broadcast = new ChatBroadcast(
                sender,
                text,
                LocalTime.now().format(CLOCK));

        session.getClients().broadcast(broadcast);
    }
}
