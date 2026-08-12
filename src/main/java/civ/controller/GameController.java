package civ.controller;

import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Unit;
import civ.model.Worker;
import civ.view.ActionPanel;
import civ.view.HudPanel;
import civ.view.MapPanel;

/**
 * Glue between clicks and the model. Every method asks the model to do something,
 * then tells the view to redraw. No game rules live here.
 */
public class GameController {

    private final Game game;
    private final MapPanel mapPanel;
    private final HudPanel hudPanel;
    private final ActionPanel actionPanel;

    public GameController(Game game, MapPanel mapPanel, HudPanel hudPanel, ActionPanel actionPanel) {
        this.game = game;
        this.mapPanel = mapPanel;
        this.hudPanel = hudPanel;
        this.actionPanel = actionPanel;
    }

    public void refresh() {
        hudPanel.refresh();
        actionPanel.refresh();
        mapPanel.repaint();
    }

    public void onHexClicked(Hex hex) {
        if (mapPanel.isAnimating()) {
            return;
        }

        Unit unitOnHex = game.unitAt(hex);
        Unit selected = game.getSelected();

        if (selected != null && game.canMove(selected, hex)) {
            int oldCol = selected.getCol();
            int oldRow = selected.getRow();
            game.moveUnit(selected, hex);
            mapPanel.invalidateMap();
            mapPanel.animateMove(selected, oldCol, oldRow);
            refresh();
            return;
        }

        if (unitOnHex != null) {
            game.select(unitOnHex);
        } else {
            game.select(null);
        }
        refresh();
    }

    public void build(Builder builder, BuildingType type) {
        game.build(builder, type, game.hexOf(builder));
        mapPanel.invalidateMap();
        refresh();
    }

    public void station(Worker worker) {
        game.station(worker);
        mapPanel.invalidateMap();
        refresh();
    }

    public void unstation(Worker worker) {
        game.unstation(worker);
        mapPanel.invalidateMap();
        refresh();
    }
}
