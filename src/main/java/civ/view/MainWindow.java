package civ.view;

import civ.controller.LobbyController;
import civ.controller.SaveController;
import civ.model.Game;
import civ.net.client.NetworkManager;
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
        ChatPanel chatPanel = new ChatPanel(text -> holder[0].sendChat(text));
        lobbyPanel = new LobbyPanel(
                chatPanel,
                ready -> holder[0].setReady(ready),
                map -> holder[0].selectMap(map),
                () -> holder[0].startGame(),
                this::leaveLobby);
        holder[0] = new LobbyController(
                networkManager,
                lobbyPanel,
                chatPanel,
                this::onConnectionLost);
        lobbyController = holder[0];

        root.add(lobbyPanel, "lobby");
        networkManager.connect(host, port);
        lobbyController.joinLobby(username);
        cards.show(root, "lobby");
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
        if (networkManager != null) {
            networkManager.disconnect();
            networkManager = null;
        }
        if (hostedServer != null) {
            hostedServer.stop();
            hostedServer = null;
        }
        lobbyController = null;
        if (lobbyPanel != null) {
            root.remove(lobbyPanel);
            lobbyPanel = null;
        }
    }
}
