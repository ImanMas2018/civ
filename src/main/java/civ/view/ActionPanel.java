package civ.view;

import civ.controller.GameController;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Tech;
import civ.model.Unit;
import civ.model.UnitBlueprint;
import civ.model.Worker;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Side panel for the selected unit. Illegal actions are greyed out with a tooltip
 * that says why — the player never clicks something that then fails.
 */
public class ActionPanel extends JPanel {

    private static final Color BG = new Color(32, 36, 46);
    private static final Color TITLE = new Color(230, 235, 245);
    private static final Color BODY = new Color(180, 190, 210);

    private final Game game;
    private GameController controller;

    public ActionPanel(Game game) {
        this.game = game;
        setPreferredSize(new Dimension(250, 0));
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void refresh() {
        removeAll();

        Unit selected = game.getSelected();
        if (selected == null) {
            addTitle("Nothing selected");
            addBody("Click a unit on the map.");
            addBody("Click empty ground to deselect.");
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
        for (UnitBlueprint blueprint : UnitBlueprint.values()) {
            addButton("Train " + blueprint.getLabel() + " (" + blueprint.getTurns() + "t)",
                    game.canTrain(blueprint),
                    "Town Hall busy, unit cap reached, or not enough food/wood.",
                    () -> controller.train(blueprint));
        }
        for (Tech tech : Tech.values()) {
            if (game.getEmpire().hasTech(tech)) {
                continue;
            }
            addButton("Research " + tech.getLabel(),
                    game.canResearch(tech),
                    "Town Hall busy, prerequisite missing, or not enough resources.",
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
        if (type.getRequiredTerrain() != null
                && game.hexOf(builder).getTerrain() != type.getRequiredTerrain()) {
            return "Needs terrain: " + type.getRequiredTerrain().getLabel();
        }
        return "The deposit, AP or resources are not enough.";
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
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setForeground(BODY);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(4));
    }

    private void addButton(String text, boolean enabled, String reason, Runnable action) {
        JButton button = new JButton(text);
        button.setEnabled(enabled);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(226, 30));
        if (!enabled) {
            button.setToolTipText(reason);
        }
        button.addActionListener(e -> action.run());
        add(button);
        add(Box.createVerticalStrut(4));
    }
}
