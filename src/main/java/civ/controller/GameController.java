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
import civ.net.client.NetworkManager;
import civ.net.protocol.request.BuildRequest;
import civ.net.protocol.request.BuildRoadRequest;
import civ.net.protocol.request.BuildWallRequest;
import civ.net.protocol.request.CancelTownHallOrderRequest;
import civ.net.protocol.request.DemolishRequest;
import civ.net.protocol.request.DemolishWallRequest;
import civ.net.protocol.request.EndTurnRequest;
import civ.net.protocol.request.ExpandBorderRequest;
import civ.net.protocol.request.FoundTownHallRequest;
import civ.net.protocol.request.MoveUnitRequest;
import civ.net.protocol.request.ResearchRequest;
import civ.net.protocol.request.StationRequest;
import civ.net.protocol.request.TrainRequest;
import civ.net.protocol.request.UnstationRequest;
import civ.net.protocol.request.UpgradeTownHallRequest;
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

public class GameController {

    private final Game game;
    private final MapPanel mapPanel;
    private final HudPanel hudPanel;
    private final ActionPanel actionPanel;
    private final TurnEngine turnEngine = new TurnEngine();
    private final NetworkManager network;

    private boolean placingWall;
    private boolean demolishingWall;
    private boolean attacking;
    private boolean attackingWall;

    public GameController(Game game, MapPanel mapPanel, HudPanel hudPanel, ActionPanel actionPanel) {
        this(game, mapPanel, hudPanel, actionPanel, null);
    }

    public GameController(Game game, MapPanel mapPanel, HudPanel hudPanel, ActionPanel actionPanel,
                          NetworkManager network) {
        this.game = game;
        this.mapPanel = mapPanel;
        this.hudPanel = hudPanel;
        this.actionPanel = actionPanel;
        this.network = network;
    }

    public boolean isNetworked() {
        return network != null;
    }

    public boolean isMyTurn() {
        if (!isNetworked()) {
            return true;
        }
        return game.isTurnOf(game.getViewpointPlayer());
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
            if (isNetworked()) {
                attacking = false;
                refresh();
                return;
            }
            attacking = false;
            Hex from = game.hexOf(selected);
            if (game.isDiceAttack(from, hex)) {
                BattleReport report = game.beginDiceAttack(from, hex);
                if (report != null) {
                    BattlePanel.showAnimated(mapPanel, report, () -> {
                        game.applyPendingDiceAttack();
                        mapPanel.invalidateMap();
                        refresh();
                        checkVictory();
                    });
                }
            } else if (game.canAttack(from, hex)) {
                game.performQuietAttack(from, hex);
                mapPanel.invalidateMap();
            }
            refresh();
            checkVictory();
            return;
        }
        if (attackingWall && selected instanceof MilitaryUnit) {
            attackingWall = false;
            if (!isNetworked()) {
                game.attackWall(game.hexOf(selected), hex);
                mapPanel.invalidateMap();
            }
            refresh();
            return;
        }

        if (placingWall && selected instanceof Builder) {
            placingWall = false;
            if (isNetworked()) {
                if (!isMyTurn()) {
                    refresh();
                    return;
                }
                network.send(new BuildWallRequest(selected.getId(), hex.getCol(), hex.getRow()));
            } else {
                game.buildWall((Builder) selected, hex);
                mapPanel.invalidateMap();
            }
            refresh();
            return;
        }
        if (demolishingWall && selected instanceof Builder) {
            demolishingWall = false;
            if (isNetworked()) {
                if (!isMyTurn()) {
                    refresh();
                    return;
                }
                network.send(new DemolishWallRequest(selected.getId(), hex.getCol(), hex.getRow()));
            } else {
                game.demolishWall((Builder) selected, hex);
                mapPanel.invalidateMap();
            }
            refresh();
            return;
        }

        if (selected instanceof BorderExpander
                && game.canExpandBorder((BorderExpander) selected, hex)) {
            if (isNetworked()) {
                if (!isMyTurn()) {
                    return;
                }
                network.send(new ExpandBorderRequest(selected.getId(), hex.getCol(), hex.getRow()));
            } else {
                game.expandBorder((BorderExpander) selected, hex);
                mapPanel.invalidateMap();
            }
            refresh();
            return;
        }

        if (selected != null && game.canMove(selected, hex)) {
            if (isNetworked()) {
                if (!isMyTurn()) {
                    return;
                }
                network.send(new MoveUnitRequest(selected.getId(), hex.getCol(), hex.getRow()));
            } else {
                int oldCol = selected.getCol();
                int oldRow = selected.getRow();
                game.moveUnit(selected, hex);
                mapPanel.invalidateMap();
                mapPanel.animateMove(selected, oldCol, oldRow);
            }
            refresh();
            return;
        }

        Tribe tribe = game.tribeAt(hex);
        if (tribe != null && tribe.isDiscovered() && !tribe.isDestroyed()) {
            if (!isNetworked()) {
                openTribePanel(tribe);
            }
            game.select(null);
            refresh();
            return;
        }

        List<Unit> here = game.unitsAt(hex);
        List<Unit> mine = new java.util.ArrayList<>();
        for (Unit unit : here) {
            if (game.owns(game.getViewpointPlayer(), unit)) {
                mine.add(unit);
            }
        }
        if (mine.isEmpty()) {
            game.select(null);
        } else if (selected != null && selected.isOn(hex)
                && game.owns(game.getViewpointPlayer(), selected)) {
            int index = mine.indexOf(selected);
            if (index < 0) {
                game.select(mine.get(0));
            } else {
                game.select(mine.get((index + 1) % mine.size()));
            }
        } else {
            game.select(mine.get(0));
        }
        refresh();
    }

    public void build(Builder builder, BuildingType type) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            Hex hex = game.hexOf(builder);
            network.send(new BuildRequest(builder.getId(), type.name(), hex.getCol(), hex.getRow()));
            return;
        }
        game.build(builder, type, game.hexOf(builder));
        mapPanel.invalidateMap();
        refresh();
    }

    public void foundTownHall(Builder builder) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            Hex hex = game.hexOf(builder);
            network.send(new FoundTownHallRequest(builder.getId(), hex.getCol(), hex.getRow()));
            return;
        }
        game.foundTownHall(builder, game.hexOf(builder));
        mapPanel.invalidateMap();
        refresh();
        checkVictory();
    }

    public void station(Worker worker) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new StationRequest(worker.getId()));
            return;
        }
        game.station(worker);
        mapPanel.invalidateMap();
        refresh();
    }

    public void unstation(Worker worker) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new UnstationRequest(worker.getId()));
            return;
        }
        game.unstation(worker);
        mapPanel.invalidateMap();
        refresh();
    }

    public void buildRoad(Builder builder) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new BuildRoadRequest(builder.getId()));
            return;
        }
        game.buildRoad(builder);
        mapPanel.invalidateMap();
        refresh();
    }

    public void startPlaceWall() {
        if (isNetworked() && !isMyTurn()) {
            return;
        }
        placingWall = true;
        demolishingWall = false;
        attacking = false;
        attackingWall = false;
        refresh();
    }

    public void startDemolishWall() {
        if (isNetworked() && !isMyTurn()) {
            return;
        }
        demolishingWall = true;
        placingWall = false;
        attacking = false;
        attackingWall = false;
        refresh();
    }

    public void startAttack() {
        if (isNetworked()) {
            return;
        }
        attacking = true;
        attackingWall = false;
        placingWall = false;
        demolishingWall = false;
        refresh();
    }

    public void startAttackWall() {
        if (isNetworked()) {
            return;
        }
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
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new DemolishRequest(builder.getId(), hex.getCol(), hex.getRow()));
            return;
        }
        game.demolish(builder, hex);
        mapPanel.invalidateMap();
        refresh();
    }

    public void train(UnitBlueprint blueprint) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new TrainRequest(blueprint.name()));
            return;
        }
        game.train(blueprint);
        refresh();
    }

    public void research(Tech tech) {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new ResearchRequest(tech.name()));
            return;
        }
        game.research(tech);
        refresh();
    }

    public void cancelTownHallOrder() {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new CancelTownHallOrderRequest());
            return;
        }
        game.cancelTownHallOrder();
        refresh();
    }

    public void upgradeTownHall() {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
            network.send(new UpgradeTownHallRequest());
            return;
        }
        game.upgradeTownHall();
        refresh();
    }

    public void endTurn() {
        if (isNetworked()) {
            if (!isMyTurn()) {
                return;
            }
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
            network.send(new EndTurnRequest());
            return;
        }
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
        game.setProcessingTurn(true);
        try {
            turnEngine.endTurn(game);
        } finally {
            game.setProcessingTurn(false);
        }
        game.select(null);
        mapPanel.invalidateMap();
        refresh();
        checkVictory();
    }

    private void checkVictory() {
        if (isNetworked()) {
            return;
        }
        civ.model.Player winner = game.findWinner();
        if (winner != null && game.getPlayers().size() > 1) {
            JOptionPane.showMessageDialog(mapPanel,
                    winner.getName() + " wins!",
                    "Victory",
                    JOptionPane.INFORMATION_MESSAGE);
        }
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
        if (isNetworked()) {
            return;
        }
        civ.view.TradeDialog.showBazaar(mapPanel, game, this::refresh);
    }

    public void openTradingPost() {
        if (isNetworked()) {
            return;
        }
        if (game.findOwnedTradingPost() == null) {
            JOptionPane.showMessageDialog(mapPanel,
                    "Claim the Trading Post hex inside your border first.");
            return;
        }
        civ.view.TradeDialog.showTradingPost(mapPanel, game, this::refresh);
    }
}
