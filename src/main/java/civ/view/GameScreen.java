package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.event.GameEvent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class GameScreen extends JPanel {

    private final MainWindow window;
    private final Game game;
    private GameController controller;
    private MapPanel mapPanel;

    public GameScreen(Game game, MainWindow window) {
        this.game = game;
        this.window = window;
        setLayout(new BorderLayout());

        HudPanel hudPanel = new HudPanel(game);
        mapPanel = new MapPanel(game);
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
        game.getBus().subscribe(GameEvent.UNIT_KILLED, payload -> {
            mapPanel.invalidateMap();
            controller.refresh();
        });
        game.getBus().subscribe(GameEvent.RELATION_CHANGED, payload -> controller.refresh());
        game.getBus().subscribe(GameEvent.SEASON_CHANGED, payload -> {
            mapPanel.onSeasonChanged();
            controller.refresh();
        });
        game.getBus().subscribe(GameEvent.DISASTER_HAPPENED, payload -> {
            civ.model.world.DisasterEffect effect = game.getLastDisaster();
            if (effect == null) {
                controller.refresh();
                return;
            }
            game.setDisasterBusy(true);
            mapPanel.invalidateMap();
            mapPanel.playDisaster(effect, () -> {
                game.setDisasterBusy(false);
                mapPanel.invalidateMap();
                controller.refresh();
            });
            controller.refresh();
        });

        add(hudPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);

        JScrollPane actions = new JScrollPane(actionPanel);
        actions.setPreferredSize(new Dimension(300, 0));
        actions.setBorder(null);
        actions.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        actions.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        actions.getVerticalScrollBar().setUnitIncrement(16);
        add(actions, BorderLayout.EAST);

        controller.refresh();
    }

    /**
     * Esc / Back: cancel attack/wall pick, then drop the selected unit, then pause menu.
     */
    public void goBack() {
        if (controller != null && controller.cancelTransientMode()) {
            return;
        }
        if (game.getSelected() != null) {
            game.select(null);
            controller.refresh();
            return;
        }
        openPauseMenu();
    }

    private void openPauseMenu() {
        JDialog dialog = new JDialog(window, "Pause", true);
        dialog.setContentPane(new SaveLoadPanel(
                window.getSaveController(),
                game,
                true,
                dialog::dispose,
                window::showMenu));
        dialog.setSize(720, 420);
        dialog.setLocationRelativeTo(window);
        dialog.setVisible(true);
        if (controller != null) {
            controller.refresh();
        }
    }
}
