package civ.controller;

import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Tech;
import civ.model.TurnEngine;
import civ.model.Unit;
import civ.model.UnitBlueprint;
import civ.model.Worker;
import civ.view.ActionPanel;
import civ.view.HudPanel;
import civ.view.MapPanel;
import javax.swing.JOptionPane;
import java.util.List;

/**
 * Glue between clicks and the model. Every method asks the model to do something,
 * then tells the view to redraw. No game rules live here.
 */
public class GameController {

    private final Game game;
    private final MapPanel mapPanel;
    private final HudPanel hudPanel;
    private final ActionPanel actionPanel;
    private final TurnEngine turnEngine = new TurnEngine();

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

        Unit selected = game.getSelected();

        if (selected instanceof BorderExpander
                && game.canExpandBorder((BorderExpander) selected, hex)) {
            game.expandBorder((BorderExpander) selected, hex);
            mapPanel.invalidateMap();
            refresh();
            return;
        }

        if (selected != null && game.canMove(selected, hex)) {
            int oldCol = selected.getCol();
            int oldRow = selected.getRow();
            game.moveUnit(selected, hex);
            mapPanel.invalidateMap();
            mapPanel.animateMove(selected, oldCol, oldRow);
            refresh();
            return;
        }

        List<Unit> here = game.unitsAt(hex);
        if (here.isEmpty()) {
            game.select(null);
        } else if (selected != null && selected.isOn(hex)) {
            game.select(game.nextUnitOn(hex, selected));
        } else {
            game.select(here.get(0));
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

    public void train(UnitBlueprint blueprint) {
        game.train(blueprint);
        refresh();
    }

    public void research(Tech tech) {
        game.research(tech);
        refresh();
    }

    public void cancelTownHallOrder() {
        game.cancelTownHallOrder();
        refresh();
    }

    public void upgradeTownHall() {
        game.upgradeTownHall();
        refresh();
    }

    public void endTurn() {
        if (game.hasIdleUnitWithAp()) {
            int answer = JOptionPane.showConfirmDialog(
                    mapPanel,
                    "Some units still have action points left. End the turn anyway?",
                    "Idle units",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        turnEngine.endTurn(game);
        game.select(null);
        mapPanel.invalidateMap();
        refresh();
    }
}
