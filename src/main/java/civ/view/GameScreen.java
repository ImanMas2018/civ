package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Puts the map and the unit side panel together, then wires the controller. */
public class GameScreen extends JPanel {

    public GameScreen(Game game) {
        setLayout(new BorderLayout());

        MapPanel mapPanel = new MapPanel(game);
        ActionPanel actionPanel = new ActionPanel(game);

        GameController controller = new GameController(game, mapPanel, actionPanel);
        mapPanel.setController(controller);

        add(mapPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);

        controller.refresh();
    }
}
