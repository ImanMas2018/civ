package civ.view;

import civ.controller.GameController;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/**
 * Side panel for the selected unit. Illegal actions are greyed out with a tooltip
 * that says why — the player never clicks something that then fails.
 */
public class ActionPanel extends JPanel {

    private static final int PANEL_WIDTH = 280;
    private static final int LABEL_WIDTH = 236;

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
        return new Dimension(PANEL_WIDTH, super.getMinimumSize().height);
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
            addTitle("Nothing selected");
            addBody("Click a unit on the map.");
            addBody("Click a hex to inspect its terrain.");
        } else {
            addTitle(selected.describe());
            addBody("Vision  " + selected.getVisionRadius());

            if (selected instanceof Builder) {
                Builder builder = (Builder) selected;
                add(Box.createVerticalStrut(8));
                addTitle("Build here");
                for (BuildingType type : BuildingType.values()) {
                    if (type == BuildingType.TOWN_HALL) {
                        continue;
                    }
                    boolean allowed = game.canBuild(builder, type, game.hexOf(builder));
                    addButton(type.getLabel() + "  (" + type.getWoodCost() + "w "
                                    + type.getStoneCost() + "s)",
                            allowed,
                            reasonWhyNot(builder, type),
                            () -> controller.build(builder, type));
                }
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
        }

        add(Box.createVerticalStrut(12));
        addTitle("Town Hall");
        TownHall townHall = game.getEmpire().getTownHall();
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
            if (game.getEmpire().hasTech(tech)) {
                continue;
            }
            addButton("Research " + tech.getLabel(),
                    game.canResearch(tech),
                    researchReason(tech),
                    () -> controller.research(tech));
        }

        revalidate();
        repaint();
    }

    private String reasonWhyNot(Builder builder, BuildingType type) {
        if (!builder.hasCharge()) {
            return "This builder has no charges left.";
        }
        if (!game.hexOf(builder).isOwned()) {
            return "This hex is outside your border.";
        }
        if (game.hexOf(builder).getBuilding() != null) {
            return "This hex already has a building.";
        }
        if (type.getRequiredTech() != null
                && !game.getEmpire().hasTech(type.getRequiredTech())) {
            return "Needs the technology: " + type.getRequiredTech().getLabel();
        }
        if (type.getRequiredLevel() > game.getEmpire().getTownHall().getLevel()) {
            return "Needs Town Hall level " + type.getRequiredLevel();
        }
        if (type.getRequiredTerrain() != null
                && game.hexOf(builder).getTerrain() != type.getRequiredTerrain()) {
            return "Needs terrain: " + type.getRequiredTerrain().getLabel();
        }
        return "The deposit, AP or resources are not enough.";
    }

    private String trainReason(UnitBlueprint blueprint) {
        if (game.getEmpire().getTownHall().isBusy()) {
            return "Town Hall is busy. Cancel the current order first.";
        }
        if (blueprint.getRequiredLevel() > game.getEmpire().getTownHall().getLevel()) {
            return "Needs Town Hall level " + blueprint.getRequiredLevel();
        }
        if (blueprint.isMilitary()
                && game.getEmpire().countMilitary() >= game.getEmpire().getMilitaryCap()) {
            return "Military unit cap reached.";
        }
        return "Unit cap reached, or not enough food/wood.";
    }

    private String researchReason(Tech tech) {
        if (game.getEmpire().getTownHall().isBusy()) {
            return "Town Hall is busy. Cancel the current order first.";
        }
        if (tech.getRequiredLevel() > game.getEmpire().getTownHall().getLevel()) {
            return "Needs Town Hall level " + tech.getRequiredLevel();
        }
        if (tech.getRequired() != null && !game.getEmpire().hasTech(tech.getRequired())) {
            return "Needs " + tech.getRequired().getLabel() + " first.";
        }
        return "Not enough resources.";
    }

    private void addTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TITLE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(6));
    }

    private void addBody(String text) {
        JLabel label = new JLabel("<html><div style='width:" + LABEL_WIDTH + "px'>"
                + text + "</div></html>");
        label.setForeground(BODY);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(4));
    }

    private void addButton(String text, boolean enabled, String reason, Runnable action) {
        JButton button = new JButton("<html><div style='text-align:center; width:"
                + LABEL_WIDTH + "px'>" + text + "</div></html>");
        button.setEnabled(enabled);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(6, 8, 6, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        if (!enabled) {
            button.setToolTipText(reason);
        }
        button.addActionListener(e -> action.run());
        add(button);
        add(Box.createVerticalStrut(4));
    }
}
