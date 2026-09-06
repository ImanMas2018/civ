package civ.controller;

import civ.net.client.NetworkManager;
import civ.net.protocol.Message;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.request.SelectMapRequest;
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.view.ChatPanel;
import civ.view.LobbyPanel;
import java.util.function.Consumer;
import javax.swing.JOptionPane;

/** Owns lobby UI updates. Talks only to NetworkManager, never to java.net. */
public class LobbyController {

    private final NetworkManager network;
    private final LobbyPanel lobbyPanel;
    private final ChatPanel chatPanel;
    private final Consumer<String> onFatalDisconnect;

    private String localName = "";

    public LobbyController(NetworkManager network,
                           LobbyPanel lobbyPanel,
                           ChatPanel chatPanel,
                           Consumer<String> onFatalDisconnect) {
        this.network = network;
        this.lobbyPanel = lobbyPanel;
        this.chatPanel = chatPanel;
        this.onFatalDisconnect = onFatalDisconnect;

        network.setOnMessage(this::onMessage);
        network.setOnConnectionLost(() ->
                onFatalDisconnect.accept("Connection to the server was lost"));
    }

    public void joinLobby(String username) {
        this.localName = username;
        lobbyPanel.setLocalName(username);
        chatPanel.clear();
        network.send(new JoinRequest(username));
    }

    public void setReady(boolean ready) {
        network.send(new ReadyRequest(ready));
    }

    public void selectMap(String mapName) {
        network.send(new SelectMapRequest(mapName));
    }

    public void startGame() {
        network.send(new StartGameRequest());
    }

    public void sendChat(String text) {
        network.send(new ChatRequest(text));
    }

    public void leave() {
        network.disconnect();
    }

    private void onMessage(Message message) {
        if (message instanceof LobbyStateBroadcast) {
            lobbyPanel.applyState((LobbyStateBroadcast) message);
        } else if (message instanceof ChatBroadcast) {
            ChatBroadcast chat = (ChatBroadcast) message;
            chatPanel.append(chat.getTime(), chat.getSender(), chat.getText());
        } else if (message instanceof NoticePush) {
            lobbyPanel.showNotice(((NoticePush) message).getText());
        } else if (message instanceof ErrorResponse) {
            String reason = ((ErrorResponse) message).getReason();
            JOptionPane.showMessageDialog(lobbyPanel, reason, "Server", JOptionPane.WARNING_MESSAGE);
            lobbyPanel.showNotice(reason);
        }
    }
}
