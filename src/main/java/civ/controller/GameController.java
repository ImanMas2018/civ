package civ.controller;

import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Tech;
import civ.model.TurnEngine;
import civ.model.Unit;
import civ.model.UnitBlueprint;
import civ.model.Worker;
import civ.model.combat.BattleReport;
import civ.model.tribe.Tribe;
import civ.view.ActionPanel;
import civ.view.BattlePanel;
import civ.view.HudPanel;
import civ.view.MapPanel;
import civ.view.TribePanel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import java.awt.Dimension;
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
    private boolean placingWall;
    private boolean demolishingWall;
    private boolean attacking;
    private boolean attackingWall;

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

        game.inspect(hex);

        Unit selected = game.getSelected();

        if (attacking && selected instanceof MilitaryUnit) {
            attacking = false;
            Hex from = game.hexOf(selected);
            if (game.isDiceAttack(from, hex)) {
                BattleReport report = game.beginDiceAttack(from, hex);
                if (report != null) {
                    BattlePanel.showAnimated(mapPanel, report, () -> {
                        game.applyPendingDiceAttack();
                        mapPanel.invalidateMap();
                        refresh();
                    });
                }
            } else if (game.canAttack(from, hex)) {
                game.performQuietAttack(from, hex);
                mapPanel.invalidateMap();
            }
            refresh();
            return;
        }
        if (attackingWall && selected instanceof MilitaryUnit) {
            attackingWall = false;
            game.attackWall(game.hexOf(selected), hex);
            mapPanel.invalidateMap();
            refresh();
            return;
        }

        if (placingWall && selected instanceof Builder) {
            game.buildWall((Builder) selected, hex);
            placingWall = false;
            mapPanel.invalidateMap();
            refresh();
            return;
        }
        if (demolishingWall && selected instanceof Builder) {
            game.demolishWall((Builder) selected, hex);
            demolishingWall = false;
            mapPanel.invalidateMap();
            refresh();
            return;
        }

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

        Tribe tribe = game.tribeAt(hex);
        if (tribe != null && tribe.isDiscovered() && !tribe.isDestroyed()) {
            openTribePanel(tribe);
            game.select(null);
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

    public void buildRoad(Builder builder) {
        game.buildRoad(builder);
        mapPanel.invalidateMap();
        refresh();
    }

    public void startPlaceWall() {
        placingWall = true;
        demolishingWall = false;
        attacking = false;
        attackingWall = false;
        refresh();
    }

    public void startDemolishWall() {
        demolishingWall = true;
        placingWall = false;
        attacking = false;
        attackingWall = false;
        refresh();
    }

    public void startAttack() {
        attacking = true;
        attackingWall = false;
        placingWall = false;
        demolishingWall = false;
        refresh();
    }

    public void startAttackWall() {
        attackingWall = true;
        attacking = false;
        placingWall = false;
        demolishingWall = false;
        refresh();
    }

    public boolean isPlacingWall() {
        return placingWall;
    }

    public boolean isDemolishingWall() {
        return demolishingWall;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public boolean isAttackingWall() {
        return attackingWall;
    }

    /** Clears wall/attack pick modes. True if something was cancelled. */
    public boolean cancelTransientMode() {
        if (!placingWall && !demolishingWall && !attacking && !attackingWall) {
            return false;
        }
        placingWall = false;
        demolishingWall = false;
        attacking = false;
        attackingWall = false;
        refresh();
        return true;
    }

    public void demolish(Builder builder, Hex hex) {
        int answer = JOptionPane.showConfirmDialog(
                mapPanel,
                "Demolish this? Spent resources are not returned.",
                "Demolish",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        game.demolish(builder, hex);
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

    public void openTribePanel(Tribe tribe) {
        JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(mapPanel),
                tribe.getName(), JDialog.ModalityType.MODELESS);
        TribePanel panel = new TribePanel(game, tribe, this);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(320, 520));
        dialog.setContentPane(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(mapPanel);
        dialog.setVisible(true);
    }

    public void openBazaar() {
        civ.view.TradeDialog.showBazaar(mapPanel, game, this::refresh);
    }

    public void openTradingPost() {
        if (game.findOwnedTradingPost() == null) {
            JOptionPane.showMessageDialog(mapPanel,
                    "Claim the Trading Post hex inside your border first.");
            return;
        }
        civ.view.TradeDialog.showTradingPost(mapPanel, game, this::refresh);
    }
}
