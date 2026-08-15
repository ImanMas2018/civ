package civ.view;

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

/**
 * Top-level window. Uses CardLayout to switch between menu and game screens.
 */
public class MainWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MusicPlayer music = new MusicPlayer();
    private GameScreen gameScreen;

    public MainWindow() {
        setTitle("Civ — Advanced Programming");
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

    /** Called when the player presses Start. Opens a new map. */
    public void startNewGame() {
        // Drop the previous screen, otherwise every Start press leaves another
        // full map panel behind under the same card name.
        if (gameScreen != null) {
            root.remove(gameScreen);
        }
        Game game = new Game(System.currentTimeMillis());
        gameScreen = new GameScreen(game, this);
        root.add(gameScreen, "game");
        cards.show(root, "game");
    }

    public void showMenu() {
        cards.show(root, "menu");
    }
}
