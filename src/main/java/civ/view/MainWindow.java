package civ.view;

import civ.controller.LobbyController;
import civ.controller.SaveController;
import civ.model.Game;
import civ.net.client.NetworkManager;
import civ.net.client.SnapshotApplier;
import civ.net.protocol.dto.GameStateDto;
import civ.net.server.GameServer;
import civ.util.MusicPlayer;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class MainWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MusicPlayer music = new MusicPlayer();
    private final SaveController saveController = new SaveController(this);
    private GameScreen gameScreen;

    private ConnectPanel connectPanel;
    private LobbyPanel lobbyPanel;
    private LobbyController lobbyController;
    private NetworkManager networkManager;
    private GameServer hostedServer;
    private ChatPanel networkChatPanel;

    public MainWindow() {
        setTitle("Civ — AP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);

        root.add(new MenuPanel(this), "menu");
        setContentPane(root);
        cards.show(root, "menu");

        music.play("/music.wav");
        music.setVolume(60);
        installEscapeBack();
        installDebugDump();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownNetwork();
            }
        });
    }

    /** Esc works no matter which panel has focus. */
    private void installEscapeBack() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getRootPane().getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameScreen != null && gameScreen.isShowing()) {
                    gameScreen.goBack();
                }
            }
        });
    }

    /** F3 dumps the last fog-filtered GameStateDto (anti-cheat demo). */
    private void installDebugDump() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "dumpDto");
        getRootPane().getActionMap().put("dumpDto", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lobbyController == null) {
                    return;
                }
                String json = lobbyController.dumpSnapshotJson();
                System.out.println("=== GameStateDto dump ===");
                System.out.println(json);
                JOptionPane.showMessageDialog(MainWindow.this,
                        "Fog-filtered snapshot printed to console ("
                                + json.length() + " chars).",
                        "Debug dump",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public MusicPlayer getMusic() {
        return music;
    }

    public SaveController getSaveController() {
        return saveController;
    }

    /** Called when the player presses Start. Opens a new map. */
    public void startNewGame() {
        Game game = new Game(System.currentTimeMillis());
        saveController.wireAutosave(game);
        openGame(game);
    }

    /** Offline 2-player hot-seat on the Crossroads designed map. */
    public void startHotseatGame() {
        try {
            civ.model.map.MapPreset map = civ.model.map.MapCatalog.crossroads();
            java.util.List<String> names = java.util.List.of("Player 1", "Player 2");
            Game game = new Game(System.currentTimeMillis(), names, map);
            saveController.wireAutosave(game);
            openGame(game);
        } catch (java.io.IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not load the Crossroads map: " + ex.getMessage());
        }
    }

    public void openGame(Game game) {
        if (gameScreen != null) {
            root.remove(gameScreen);
        }
        gameScreen = new GameScreen(game, this);
        root.add(gameScreen, "game");
        cards.show(root, "game");
    }

    public void showMenu() {
        cards.show(root, "menu");
    }

    public void showLoadMenu() {
        SaveLoadPanel panel = new SaveLoadPanel(saveController, null, false, this::showMenu);
        root.add(panel, "load");
        cards.show(root, "load");
    }

    public void showConnect(boolean preferJoin) {
        if (connectPanel == null) {
            connectPanel = new ConnectPanel(
                    this::hostMultiplayer,
                    this::joinMultiplayer,
                    this::showMenu);
            root.add(connectPanel, "connect");
        }
        if (preferJoin) {
            connectPanel.preferJoinMode();
        }
        connectPanel.setStatus(" ");
        cards.show(root, "connect");
    }

    private void hostMultiplayer(int port) {
        String username = connectPanel.getUsername();
        shutdownNetwork();
        try {
            hostedServer = new GameServer(port);
            hostedServer.start();
            connectAsClient("127.0.0.1", port, username);
        } catch (IOException ex) {
            shutdownNetwork();
            connectPanel.setStatus("Could not host on port " + port + ": " + ex.getMessage());
        }
    }

    private void joinMultiplayer(String host, Integer port) {
        String username = connectPanel.getUsername();
        shutdownNetwork();
        try {
            connectAsClient(host, port, username);
        } catch (IOException ex) {
            shutdownNetwork();
            connectPanel.setStatus("Could not connect: " + ex.getMessage());
        }
    }

    private void connectAsClient(String host, int port, String username) throws IOException {
        networkManager = new NetworkManager();

        final LobbyController[] holder = new LobbyController[1];
        networkChatPanel = new ChatPanel(text -> holder[0].sendChat(text));
        lobbyPanel = new LobbyPanel(
                networkChatPanel,
                ready -> holder[0].setReady(ready),
                map -> holder[0].selectMap(map),
                enabled -> holder[0].setCheats(enabled),
                () -> holder[0].startGame(),
                this::leaveLobby);
        holder[0] = new LobbyController(
                networkManager,
                lobbyPanel,
                networkChatPanel,
                this::onConnectionLost,
                this::enterNetworkGame);
        lobbyController = holder[0];
        lobbyController.setEndpoint(host, port);

        root.add(lobbyPanel, "lobby");
        networkManager.connect(host, port);
        lobbyController.joinLobby(username);
        cards.show(root, "lobby");
    }

    private void enterNetworkGame(GameStateDto first) {
        try {
            Game game = SnapshotApplier.createShell(first);
            if (gameScreen != null) {
                root.remove(gameScreen);
            }
            // Re-parent chat into the game HUD
            if (lobbyPanel != null) {
                root.remove(lobbyPanel);
            }
            gameScreen = new GameScreen(game, this, networkManager, networkChatPanel);
            root.add(gameScreen, "game");
            cards.show(root, "game");

            lobbyController.setOnBattleReport(push ->
                    BattleReportDialog.show(gameScreen, push));
            lobbyController.setOnAlliancePrompt(prompt -> {
                int answer = JOptionPane.showConfirmDialog(gameScreen,
                        prompt.getFromPlayerName() + " proposes an alliance. Accept?",
                        "Alliance proposal",
                        JOptionPane.YES_NO_OPTION);
                gameScreen.getController().replyAlliance(
                        prompt.getFromPlayerId(), answer == JOptionPane.YES_OPTION);
            });

            lobbyController.getClientState().addListener(() -> {
                GameStateDto next = lobbyController.getClientState().get();
                if (next == null || gameScreen == null) {
                    return;
                }
                SnapshotApplier.apply(game, next);
                gameScreen.onSnapshotApplied();
            });
            // First snapshot already applied in createShell; still refresh once
            gameScreen.onSnapshotApplied();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not open the game map: " + ex.getMessage());
            leaveNetworkGame();
        }
    }

    public void leaveNetworkGame() {
        if (lobbyController != null) {
            lobbyController.leave();
        }
        shutdownNetwork();
        showMenu();
    }

    private void leaveLobby() {
        if (lobbyController != null) {
            lobbyController.leave();
        }
        shutdownNetwork();
        showMenu();
    }

    private void onConnectionLost(String message) {
        JOptionPane.showMessageDialog(this, message);
        shutdownNetwork();
        showMenu();
    }

    private void shutdownNetwork() {
        if (lobbyController != null) {
            lobbyController.stopHeartbeat();
        }
        if (networkManager != null) {
            networkManager.disconnect();
            networkManager = null;
        }
        if (hostedServer != null) {
            hostedServer.stop();
            hostedServer = null;
        }
        lobbyController = null;
        networkChatPanel = null;
        if (lobbyPanel != null) {
            root.remove(lobbyPanel);
            lobbyPanel = null;
        }
        if (gameScreen != null && gameScreen.getChatPanel() != null) {
            // networked game screen goes away with network session
            root.remove(gameScreen);
            gameScreen = null;
        }
    }
}
