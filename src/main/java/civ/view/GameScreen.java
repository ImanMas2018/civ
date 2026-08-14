package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

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

        JScrollPane actions = new JScrollPane(actionPanel);
        actions.setPreferredSize(new Dimension(260, 0));
        actions.setBorder(null);
        actions.getVerticalScrollBar().setUnitIncrement(16);
        add(actions, BorderLayout.EAST);

        controller.refresh();
    }
}
