package civ.view;

import civ.controller.SaveController;
import civ.model.Game;
import civ.util.MusicPlayer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class MainWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MusicPlayer music = new MusicPlayer();
    private final SaveController saveController = new SaveController(this);
    private GameScreen gameScreen;

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
}
