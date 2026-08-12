package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Puts the HUD, map and action panel together, then wires the controller. */
public class GameScreen extends JPanel {

    public GameScreen(Game game) {
        setLayout(new BorderLayout());

        HudPanel hudPanel = new HudPanel(game);
        MapPanel mapPanel = new MapPanel(game);
        ActionPanel actionPanel = new ActionPanel(game);

        GameController controller = new GameController(game, mapPanel, hudPanel, actionPanel);
        mapPanel.setController(controller);
        hudPanel.setController(controller);
        actionPanel.setController(controller);

        add(hudPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);

        controller.refresh();
    }
}
