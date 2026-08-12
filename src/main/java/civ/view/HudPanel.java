package civ.view;

import civ.controller.GameController;
import civ.model.Empire;
import civ.model.Game;
import civ.model.ResourceType;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;

/**
 * Permanent top bar: turn, resources as current/capacity plus net rate, unit cap.
 * End Turn and the Town Hall queue arrive in Step 5.
 */
public class HudPanel extends JPanel {

    private static final Color BG = new Color(24, 28, 36);
    private static final Color TEXT = Color.WHITE;
    private static final Color NEGATIVE = new Color(255, 110, 110);

    private final Game game;

    private final JLabel turnLabel = new JLabel();
    private final JLabel unitsLabel = new JLabel();
    private final JPanel resourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));

    public HudPanel(Game game) {
        this.game = game;
        setLayout(new FlowLayout(FlowLayout.LEFT, 16, 6));
        setBackground(BG);

        turnLabel.setForeground(TEXT);
        unitsLabel.setForeground(TEXT);
        turnLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        unitsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resourcePanel.setOpaque(false);

        add(turnLabel);
        add(resourcePanel);
        add(unitsLabel);
    }

    public void setController(GameController controller) {
        // End Turn is wired here in Step 5.
    }

    public void refresh() {
        Empire empire = game.getEmpire();
        Map<ResourceType, Integer> rate = empire.netRatePerTurn();

        turnLabel.setText("Turn " + game.getTurn());

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
                + "  (E" + empire.countUnits("Explorer")
                + " B" + empire.countUnits("Builder")
                + " W" + empire.countUnits("Worker") + ")");

        revalidate();
        repaint();
    }
}
