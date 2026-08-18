package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.event.GameEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Puts the HUD, map and action panel together, then wires the controller. */
public class GameScreen extends JPanel {

    private final MainWindow window;
    private final Game game;
    private GameController controller;

    public GameScreen(Game game, MainWindow window) {
        this.game = game;
        this.window = window;
        setLayout(new BorderLayout());

        HudPanel hudPanel = new HudPanel(game);
        MapPanel mapPanel = new MapPanel(game);
        ActionPanel actionPanel = new ActionPanel(game);

        controller = new GameController(game, mapPanel, hudPanel, actionPanel);
        mapPanel.setController(controller);
        hudPanel.setController(controller);
        actionPanel.setController(controller);
        hudPanel.setOnMenu(this::goBack);

        game.getBus().subscribe(GameEvent.TURN_ENDED, payload -> controller.refresh());
        game.getBus().subscribe(GameEvent.BUILDING_PLACED, payload -> {
            mapPanel.invalidateMap();
            controller.refresh();
        });
        game.getBus().subscribe(GameEvent.BUILDING_DESTROYED, payload -> {
            mapPanel.invalidateMap();
            controller.refresh();
        });

        add(hudPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);

        JScrollPane actions = new JScrollPane(actionPanel);
        actions.setPreferredSize(new Dimension(300, 0));
        actions.setBorder(null);
        actions.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        actions.getVerticalScrollBar().setUnitIncrement(16);
        add(actions, BorderLayout.EAST);

        controller.refresh();
    }

    /**
     * Esc / Back: first drop the selected unit, then confirm return to the menu.
     */
    public void goBack() {
        if (game.getSelected() != null) {
            game.select(null);
            controller.refresh();
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Return to the main menu? The current game will be lost if you start a new one.",
                "Back",
                JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            window.showMenu();
        }
    }
}
