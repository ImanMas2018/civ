package civ.view;

import civ.controller.GameController;
import civ.model.Empire;
import civ.model.Game;
import civ.model.ResourceType;
import civ.model.TownHall;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;

public class HudPanel extends JPanel {

    private static final Color BG = new Color(24, 28, 36);
    private static final Color TEXT = Color.WHITE;
    private static final Color NEGATIVE = new Color(255, 110, 110);

    private final Game game;
    private GameController controller;

    private final JLabel turnLabel = new JLabel();
    private final JLabel unitsLabel = new JLabel();
    private final JLabel queueLabel = new JLabel();
    private final JLabel townHallLabel = new JLabel();
    private final JLabel warningLabel = new JLabel();
    private final JLabel logLabel = new JLabel();
    private final JPanel resourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
    private final JButton endTurnButton = new JButton("End Turn");
    private final JButton backButton = new JButton("Back");
    private Runnable onMenu;

    public HudPanel(Game game) {
        this.game = game;
        setLayout(new BorderLayout(8, 0));
        setBackground(BG);

        for (JLabel label : new JLabel[] {turnLabel, unitsLabel, queueLabel, townHallLabel, warningLabel, logLabel}) {
            label.setForeground(TEXT);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        warningLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        resourcePanel.setOpaque(false);

        endTurnButton.addActionListener(e -> {
            if (controller != null) {
                controller.endTurn();
            }
        });
        backButton.addActionListener(e -> {
            if (onMenu != null) {
                onMenu.run();
            }
        });

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        row1.setOpaque(false);
        row1.add(turnLabel);
        row1.add(resourcePanel);
        row1.add(unitsLabel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        row2.setOpaque(false);
        row2.add(townHallLabel);
        row2.add(queueLabel);
        row2.add(warningLabel);

        JPanel logBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
        logBar.setOpaque(false);
        logBar.add(logLabel);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(row1);
        left.add(row2);
        left.add(logBar);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        buttonBar.setOpaque(false);
        buttonBar.add(backButton);
        buttonBar.add(endTurnButton);

        add(left, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.EAST);
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void setOnMenu(Runnable onMenu) {
        this.onMenu = onMenu;
    }

    public void refresh() {
        Empire empire = game.getViewpointPlayer().getEmpire();
        Map<ResourceType, Integer> rate = empire.netRatePerTurn(game.getMap(), game.getTribes());

        civ.model.Player current = game.getCurrentPlayer();
        turnLabel.setText("Turn " + game.getTurn()
                + "  " + game.getSeason().getLabel()
                + "  —  " + current.getName() + "'s turn");
        turnLabel.setForeground(current.getColour().getAwt());

        boolean myTurn = controller == null || controller.isMyTurn();
        endTurnButton.setEnabled(myTurn);
        endTurnButton.setToolTipText(myTurn ? null : "It is not your turn.");

        resourcePanel.removeAll();
        for (ResourceType type : ResourceType.values()) {
            int amount = empire.getStock().get(type);
            int perTurn = rate.get(type);
            JLabel label = new JLabel(type.getLabel() + " " + amount + "/"
                    + empire.getStock().getCapacity()
                    + "  (" + (perTurn >= 0 ? "+" : "") + perTurn + ")");
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setForeground(perTurn < 0 ? NEGATIVE : TEXT);
            resourcePanel.add(label);
        }

        unitsLabel.setText("Units " + empire.getUnits().size() + "/" + empire.getUnitCap()
                + "  Army " + empire.countMilitary() + "/" + empire.getMilitaryCap()
                + "  (E" + empire.countUnits("Explorer")
                + " B" + empire.countUnits("Builder")
                + " W" + empire.countUnits("Worker")
                + " X" + empire.countUnits("Border Expander")
                + " S" + empire.countUnits("Swordsman")
                + " A" + empire.countUnits("Archer")
                + " C" + empire.countUnits("Cavalry") + ")");

        TownHall townHall = empire.getTownHall();
        if (townHall != null) {
            townHallLabel.setText(townHall.describeLevel()
                    + "  HP " + townHall.getHp() + "/" + townHall.getMaxHp()
                    + "  Happy " + empire.happiness()
                    + " (" + empire.getHappiness().getLevelName() + ")");
            queueLabel.setText(townHall.describeQueue());
        } else {
            townHallLabel.setText("No Town Hall");
            queueLabel.setText("");
        }

        if (game.isStarving(game.getViewpointPlayer())) {
            warningLabel.setText("STARVATION!");
            warningLabel.setForeground(NEGATIVE);
        } else {
            warningLabel.setText("");
        }

        List<String> log = game.getLog();
        logLabel.setText(log.isEmpty() ? "" : log.get(log.size() - 1));

        revalidate();
        repaint();
    }
}
