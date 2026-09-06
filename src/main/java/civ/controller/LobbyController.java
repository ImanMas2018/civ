package civ.controller;

import civ.net.client.ClientState;
import civ.net.client.HeartbeatClient;
import civ.net.client.NetworkManager;
import civ.net.protocol.Message;
import civ.net.protocol.MessageCodec;
import civ.net.protocol.dto.GameStateDto;
import civ.net.protocol.push.AlliancePrompt;
import civ.net.protocol.push.BattleReportPush;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.GameOverBroadcast;
import civ.net.protocol.push.GameStateBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.request.SelectMapRequest;
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.view.BattleReportDialog;
import civ.view.ChatPanel;
import civ.view.LobbyPanel;
import java.util.function.Consumer;
import javax.swing.JOptionPane;

/** Owns lobby UI updates and hands off to the game when the first snapshot arrives. */
public class LobbyController {

    private final NetworkManager network;
    private final LobbyPanel lobbyPanel;
    private final ChatPanel chatPanel;
    private final Consumer<String> onFatalDisconnect;
    private final Consumer<GameStateDto> onGameStarted;
    private final ClientState clientState = new ClientState();
    private Consumer<BattleReportPush> onBattleReport = push -> {
    };
    private Consumer<AlliancePrompt> onAlliancePrompt = prompt -> {
    };

    private String localName = "";
    private String host;
    private int port;
    private HeartbeatClient heartbeatClient;
    private boolean gameStarted;

    public LobbyController(NetworkManager network,
                           LobbyPanel lobbyPanel,
                           ChatPanel chatPanel,
                           Consumer<String> onFatalDisconnect,
                           Consumer<GameStateDto> onGameStarted) {
        this.network = network;
        this.lobbyPanel = lobbyPanel;
        this.chatPanel = chatPanel;
        this.onFatalDisconnect = onFatalDisconnect;
        this.onGameStarted = onGameStarted;

        network.setOnMessage(this::onMessage);
        network.setOnConnectionLost(() ->
                onFatalDisconnect.accept("Connection to the server was lost"));
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void setOnBattleReport(Consumer<BattleReportPush> onBattleReport) {
        this.onBattleReport = onBattleReport == null ? push -> {
        } : onBattleReport;
    }

    public void setOnAlliancePrompt(Consumer<AlliancePrompt> onAlliancePrompt) {
        this.onAlliancePrompt = onAlliancePrompt == null ? prompt -> {
        } : onAlliancePrompt;
    }

    public void setEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
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
        stopHeartbeat();
        network.disconnect();
    }

    public void stopHeartbeat() {
        if (heartbeatClient != null) {
            heartbeatClient.stop();
            heartbeatClient = null;
        }
    }

    /** Dump the last fog-filtered snapshot as compact JSON (debug / evaluation). */
    public String dumpSnapshotJson() {
        GameStateDto snapshot = clientState.get();
        if (snapshot == null) {
            return "{}";
        }
        return new MessageCodec().encode(new GameStateBroadcast(snapshot));
    }

    private void onMessage(Message message) {
        if (message instanceof LobbyStateBroadcast) {
            lobbyPanel.applyState((LobbyStateBroadcast) message);
        } else if (message instanceof ChatBroadcast) {
            ChatBroadcast chat = (ChatBroadcast) message;
            chatPanel.append(chat.getTime(), chat.getSender(), chat.getText());
        } else if (message instanceof NoticePush) {
            String text = ((NoticePush) message).getText();
            if (gameStarted) {
                // In-game notices stay visible via chat
                chatPanel.append("--", "Server", text);
            } else {
                lobbyPanel.showNotice(text);
            }
        } else if (message instanceof GameStateBroadcast) {
            GameStateDto state = ((GameStateBroadcast) message).getState();
            boolean first = !gameStarted;
            clientState.replace(state);
            if (first) {
                gameStarted = true;
                startHeartbeat(state.yourPlayerId);
                onGameStarted.accept(state);
            }
        } else if (message instanceof GameOverBroadcast) {
            String winner = ((GameOverBroadcast) message).getWinnerName();
            JOptionPane.showMessageDialog(lobbyPanel,
                    winner + " wins!",
                    "Victory",
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (message instanceof BattleReportPush) {
            onBattleReport.accept((BattleReportPush) message);
        } else if (message instanceof AlliancePrompt) {
            onAlliancePrompt.accept((AlliancePrompt) message);
        } else if (message instanceof ErrorResponse) {
            String reason = ((ErrorResponse) message).getReason();
            JOptionPane.showMessageDialog(lobbyPanel, reason, "Server", JOptionPane.WARNING_MESSAGE);
            if (!gameStarted) {
                lobbyPanel.showNotice(reason);
            }
        }
    }

    private void startHeartbeat(long playerId) {
        stopHeartbeat();
        if (host == null) {
            return;
        }
        try {
            heartbeatClient = new HeartbeatClient(host, port, playerId);
            heartbeatClient.start();
        } catch (Exception ex) {
            System.err.println("Could not start heartbeat: " + ex.getMessage());
        }
    }
}
