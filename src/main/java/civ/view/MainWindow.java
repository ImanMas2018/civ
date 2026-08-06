package civ.view;

import civ.model.Game;
import civ.util.MusicPlayer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * Top-level window. Uses CardLayout to switch between menu and game screens.
 */
public class MainWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MusicPlayer music = new MusicPlayer();

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
    }

    public MusicPlayer getMusic() {
        return music;
    }

    /** Called when the player presses Start. Opens a new map. */
    public void startNewGame() {
        Game game = new Game(System.currentTimeMillis());
        root.add(new GameScreen(game), "game");
        cards.show(root, "game");
    }

    public void showMenu() {
        cards.show(root, "menu");
    }
}
