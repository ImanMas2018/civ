package civ.view;

import civ.controller.GameController;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Tech;
import civ.model.TownHall;
import civ.model.TownHallLevel;
import civ.model.Unit;
import civ.model.UnitBlueprint;
import civ.model.Worker;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;

public class ActionPanel extends JPanel implements Scrollable {

    private static final int PANEL_WIDTH = 280;
    /** Inner wrap width: pane 300 minus scrollbar and this panel's padding. */
    private static final int TEXT_WIDTH = 240;

    private static final Color BG = new Color(32, 36, 46);
    private static final Color TITLE = new Color(230, 235, 245);
    private static final Color BODY = new Color(180, 190, 210);

    private final Game game;
    private GameController controller;

    public ActionPanel(Game game) {
        this.game = game;
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        return new Dimension(PANEL_WIDTH, natural.height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, super.getMinimumSize().height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(PANEL_WIDTH, 400);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(16, visible.height - 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void refresh() {
        removeAll();

        Unit selected = game.getSelected();
        if (game.getInspected() != null) {
            addTitle("Hex");
            addBody(game.describeInspected());
            add(Box.createVerticalStrut(8));
        }
        if (selected == null) {
            addTitle("No unit selected");
            addBody("Click a unit on the map.");
            addBody("Click a hex to inspect its terrain.");
        } else {
            addTitle(selected.describe());
            addBody("Vision  " + selected.getVisionRadius());

            if (selected instanceof Builder) {
                Builder builder = (Builder) selected;
                add(Box.createVerticalStrut(8));
                addTitle("Build here");
                addButton("Road  (8w)",
                        game.canBuildRoad(builder),
                        "Needs owned land, 1 AP and 8 wood. Not on an existing road.",
                        () -> controller.buildRoad(builder));
                addButton("Wall — click a neighbour (15w 20s)",
                        builder.canSpend(1),
                        "Spend 1 AP after you pick the edge. Not on sea or mountain range.",
                        () -> controller.startPlaceWall());
                for (BuildingType type : BuildingType.values()) {
                    if (type == BuildingType.TOWN_HALL
                            || type == BuildingType.TRADING_POST
                            || type == BuildingType.TRIBE_CAMP
                            || type == BuildingType.OUTPOST) {
                        continue;
                    }
                    boolean allowed = game.canBuild(builder, type, game.hexOf(builder));
                    addButton(type.getLabel() + "  (" + type.getWoodCost() + "w "
                                    + type.getStoneCost() + "s)",
                            allowed,
                            reasonWhyNot(builder, type),
                            () -> controller.build(builder, type));
                }
                Hex foundHex = game.hexOf(builder);
                addButton("Found Town Hall  (200w 150s 80i 100f)",
                        game.canFoundTownHall(builder, foundHex),
                        foundTownHallReason(builder, foundHex),
                        () -> controller.foundTownHall(builder));
                Hex demolishHex = game.getInspected() != null
                        ? game.getInspected() : game.hexOf(builder);
                addButton("Demolish building/road here",
                        game.canDemolish(builder, demolishHex),
                        "Stand on or next to your building or road. Town Hall cannot be demolished.",
                        () -> controller.demolish(builder, demolishHex));
                addButton("Demolish wall — click a neighbour",
                        builder.canSpend(1),
                        "Click the hex on the other side of the wall.",
                        () -> controller.startDemolishWall());
            }

            if (selected instanceof Worker) {
                Worker worker = (Worker) selected;
                add(Box.createVerticalStrut(8));
                addTitle("Worker");
                addButton("Station in building", game.canStation(worker),
                        "Stand on a finished building that still has a free worker slot.",
                        () -> controller.station(worker));
                addButton("Leave building", worker.getStation() != null,
                        "This worker is not stationed anywhere.",
                        () -> controller.unstation(worker));
            }

            if (selected instanceof BorderExpander) {
                add(Box.createVerticalStrut(8));
                addTitle("Border Expander");
                addBody("Click a discovered hex to claim it and its 6 neighbours. The unit is then consumed.");
            }

            if (selected instanceof MilitaryUnit
                    && !((MilitaryUnit) selected).isHostile()) {
                add(Box.createVerticalStrut(8));
                addTitle("Combat");
                addButton("Attack — click a hex",
                        selected.getAp() >= 1,
                        "Needs 1 AP. Adjacent hexes use every military type; range 2 needs an Archer.",
                        () -> controller.startAttack());
                addButton("Attack wall — click a neighbour",
                        selected.getAp() >= 1,
                        "No dice. Damage is the sum of attack powers. A wall bonus applies until it falls.",
                        () -> controller.startAttackWall());
                if (controller != null && controller.isAttacking()) {
                    addBody("Click a target. Adjacent: one die per type. Range 2: one archer die. Esc cancels.");
                }
                if (controller != null && controller.isAttackingWall()) {
                    addBody("Click the hex on the other side of the wall. Esc cancels.");
                }
            }
        }

        add(Box.createVerticalStrut(12));
        addTitle("Trade");
        addButton("Open Bazaar",
                game.hasBazaar()
                        && !game.getTradeTracker().alreadyTradedThisTurn(
                        new civ.model.trade.BazaarTrade(1)),
                game.hasBazaar()
                        ? "Already used the Bazaar this turn."
                        : "Build a Bazaar (Town Hall level 2).",
                () -> controller.openBazaar());
        addButton("Trading Post",
                game.findOwnedTradingPost() != null
                        && !game.getTradeTracker().alreadyTradedThisTurn(
                        new civ.model.trade.TradingPostTrade()),
                game.findOwnedTradingPost() == null
                        ? "Own the Trading Post hex first."
                        : "Already used the Trading Post this turn.",
                () -> controller.openTradingPost());
        if (controller != null && controller.isNetworked()) {
            int inbox = game.getTradeOffers().pendingFor(game.getViewpointPlayer()).size();
            addButton("Player trade inbox" + (inbox > 0 ? " (" + inbox + ")" : ""),
                    true, null, () -> controller.openTradeInbox());
            addButton("New player trade offer",
                    controller.isMyTurn(),
                    "It is not your turn.",
                    () -> controller.openNewTradeOffer());
        }

        if (controller != null && (controller.isNetworked() || game.getPlayers().size() > 1)) {
            add(Box.createVerticalStrut(8));
            addTitle("Diplomacy");
            addButton("Open diplomacy", true, null, () -> controller.openDiplomacy());
        }

        java.util.List<civ.model.tribe.Tribe> known = new java.util.ArrayList<>();
        for (civ.model.tribe.Tribe tribe : game.getTribes()) {
            if (tribe.isDiscovered() && !tribe.isDestroyed()) {
                known.add(tribe);
            }
        }
        if (!known.isEmpty()) {
            add(Box.createVerticalStrut(8));
            addTitle("Tribes");
            for (civ.model.tribe.Tribe tribe : known) {
                addButton(tribe.getName() + " (" + tribe.getState().getName() + ")",
                        true, null, () -> controller.openTribePanel(tribe));
            }
        }

        add(Box.createVerticalStrut(12));
        addTitle("Town Hall");
        TownHall townHall = game.getViewpointPlayer().getEmpire().getTownHall();
        if (townHall == null) {
            addBody("No Town Hall.");
            revalidate();
            repaint();
            return;
        }
        addBody(townHall.describeLevel());
        addBody("HP " + townHall.getHp() + "/" + townHall.getMaxHp()
                + "  Defence " + townHall.getDefence());
        if (townHall.isBusy()) {
            addBody(townHall.describeQueue());
            addButton("Cancel order", true,
                    "Cancel the current Town Hall job. Spent resources are not returned.",
                    () -> controller.cancelTownHallOrder());
        }
        TownHallLevel nextRank = townHall.getRank().next();
        if (nextRank != null) {
            addButton("Upgrade to " + nextRank.getLabel()
                            + "<br>(" + nextRank.getTurns() + "t, "
                            + nextRank.getWoodCost() + "w "
                            + nextRank.getStoneCost() + "s "
                            + nextRank.getIronCost() + "i)",
                    game.canUpgradeTownHall(),
                    "Town Hall busy, already at max level, or not enough resources.",
                    () -> controller.upgradeTownHall());
        }
        for (UnitBlueprint blueprint : UnitBlueprint.values()) {
            addButton("Train " + blueprint.getLabel() + " (" + blueprint.getTurns() + "t)",
                    game.canTrain(blueprint),
                    trainReason(blueprint),
                    () -> controller.train(blueprint));
        }
        for (Tech tech : Tech.values()) {
            if (game.getViewpointPlayer().getEmpire().hasTech(tech)) {
                continue;
            }
            addButton("Research " + tech.getLabel()
                            + "<br>(" + tech.getTurns() + "t, "
                            + tech.getWoodCost() + "w "
                            + tech.getStoneCost() + "s "
                            + tech.getIronCost() + "i)",
                    game.canResearch(tech),
                    researchReason(tech),
                    () -> controller.research(tech));
        }

        revalidate();
        repaint();
    }

    private String foundTownHallReason(Builder builder, Hex hex) {
        if (!builder.hasCharge()) {
            return "This builder has no charges left.";
        }
        if (hex == null || !hex.getTerrain().isLand()) {
            return "Need a land hex.";
        }
        if (hex.getBuilding() != null) {
            return "This hex already has a building.";
        }
        if (game.isOwned(game.getViewpointPlayer(), hex)) {
            return "Must found outside your current borders.";
        }
        if (game.isClaimed(hex)) {
            return "This hex is already claimed.";
        }
        return "Needs 200 wood, 150 stone, 80 iron, 100 food and 1 AP.";
    }

    private String reasonWhyNot(Builder builder, BuildingType type) {
        if (!builder.hasCharge()) {
            return "This builder has no charges left.";
        }
        if (!game.isOwned(game.getViewpointPlayer(), game.hexOf(builder))) {
            return "This hex is outside your border.";
        }
        if (!game.hexOf(builder).getTerrain().isLand()) {
            return "Cannot build on sea or mountain range.";
        }
        if (game.hexOf(builder).getBuilding() != null) {
            return "This hex already has a building.";
        }
        if (type == BuildingType.DOCK && !game.isCoastal(game.hexOf(builder))) {
            return "A Dock needs a coastal land hex (next to sea).";
        }
        if (type.getRequiredTech() != null
                && !game.getViewpointPlayer().getEmpire().hasTech(type.getRequiredTech())) {
            return "Needs the technology: " + type.getRequiredTech().getLabel();
        }
        if (game.getViewpointPlayer().getEmpire().getTownHall() == null
                || type.getRequiredLevel() > game.getViewpointPlayer().getEmpire().getTownHall().getLevel()) {
            return "Needs Town Hall level " + type.getRequiredLevel();
        }
        if (type.getRequiredTerrain() != null
                && game.hexOf(builder).getTerrain() != type.getRequiredTerrain()) {
            return "Needs terrain: " + type.getRequiredTerrain().getLabel();
        }
        return "The deposit, AP or resources are not enough.";
    }

    private String trainReason(UnitBlueprint blueprint) {
        TownHall hall = game.getViewpointPlayer().getEmpire().getTownHall();
        if (hall == null) {
            return "No Town Hall.";
        }
        if (hall.isBusy()) {
            return "Town Hall is busy. Cancel the current order first.";
        }
        if (blueprint.getRequiredLevel() > hall.getLevel()) {
            return "Needs Town Hall level " + blueprint.getRequiredLevel();
        }
        if (blueprint == UnitBlueprint.CAVALRY && !game.hasMilitaryStable()) {
            return "Needs a Military Stable on the map.";
        }
        if (blueprint.isMilitary()
                && game.getViewpointPlayer().getEmpire().countMilitary() >= game.getViewpointPlayer().getEmpire().getMilitaryCap()) {
            return "Military unit cap reached.";
        }
        return "Unit cap reached, or not enough food/wood.";
    }

    private String researchReason(Tech tech) {
        TownHall hall = game.getViewpointPlayer().getEmpire().getTownHall();
        if (hall == null) {
            return "No Town Hall.";
        }
        if (hall.isBusy()) {
            return "Town Hall is busy. Cancel the current order first.";
        }
        if (tech.getRequiredLevel() > hall.getLevel()) {
            return "Needs Town Hall level " + tech.getRequiredLevel();
        }
        if (tech.getRequired() != null && !game.getViewpointPlayer().getEmpire().hasTech(tech.getRequired())) {
            return "Needs " + tech.getRequired().getLabel() + " first.";
        }
        return "Not enough resources.";
    }

    private void addTitle(String text) {
        add(wrappingText(text, TITLE, Font.BOLD, 13));
        add(Box.createVerticalStrut(6));
    }

    private void addBody(String text) {
        add(wrappingText(text, BODY, Font.PLAIN, 12));
        add(Box.createVerticalStrut(4));
    }

    private JTextArea wrappingText(String text, Color color, int style, int size) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(new Font("SansSerif", style, size));
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setSize(TEXT_WIDTH, Short.MAX_VALUE);
        int height = area.getPreferredSize().height;
        area.setPreferredSize(new Dimension(TEXT_WIDTH, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        area.setMinimumSize(new Dimension(0, height));
        return area;
    }

    private void addButton(String text, boolean enabled, String reason, Runnable action) {
        boolean myTurn = controller == null || controller.isMyTurn();
        boolean reallyEnabled = enabled && myTurn;
        String tip = !myTurn ? "It is not your turn." : reason;

        // No fixed-width HTML block: that was wider than the content area and
        // made Swing clip the left, so labels looked shifted to the right.
        JButton button = new JButton("<html><center>" + text + "</center></html>");
        button.setEnabled(reallyEnabled);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setMargin(new Insets(6, 8, 6, 8));
        button.setIconTextGap(0);

        button.setSize(TEXT_WIDTH, Short.MAX_VALUE);
        int height = Math.max(32, button.getPreferredSize().height);
        button.setPreferredSize(new Dimension(TEXT_WIDTH, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.setMinimumSize(new Dimension(0, height));

        if (!reallyEnabled) {
            button.setToolTipText(tip);
        }
        button.addActionListener(e -> action.run());
        add(button);
        add(Box.createVerticalStrut(4));
    }
}
