package civ.view;

import civ.model.Game;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Holds the map view for the running game. HUD and actions arrive in later steps. */
public class GameScreen extends JPanel {

    public GameScreen(Game game) {
        setLayout(new BorderLayout());
        MapPanel mapPanel = new MapPanel(game);
        add(mapPanel, BorderLayout.CENTER);
    }
}
