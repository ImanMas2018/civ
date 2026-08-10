package civ.view;

import civ.model.Builder;
import civ.model.Explorer;
import civ.model.Game;
import civ.model.Unit;
import civ.model.Worker;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Side panel for the selected unit. Step 3 only shows info; build/station
 * buttons arrive in Step 4 once those actions exist.
 */
public class ActionPanel extends JPanel {

    private static final Color BG = new Color(32, 36, 46);
    private static final Color TITLE = new Color(230, 235, 245);
    private static final Color BODY = new Color(180, 190, 210);

    private final Game game;

    public ActionPanel(Game game) {
        this.game = game;
        setPreferredSize(new Dimension(250, 0));
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
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
            addBody("Vision  " + selected.getVisionRadius()
                    + "    AP cost is paid by the terrain.");
            add(Box.createVerticalStrut(10));
            addTitle("Abilities");
            for (String line : abilityLines(selected)) {
                addBody(line);
            }
        }

        revalidate();
        repaint();
    }

    private String[] abilityLines(Unit unit) {
        if (unit instanceof Explorer) {
            return new String[] {
                    "Scout — largest vision (3 hexes).",
                    "Most action points (6)."
            };
        }
        if (unit instanceof Builder) {
            Builder builder = (Builder) unit;
            return new String[] {
                    "Will be able to build structures.",
                    "Build charges left: " + builder.getCharges() + "/3."
            };
        }
        if (unit instanceof Worker) {
            return new String[] {
                    "Will be able to station in a building.",
                    "Currently idle."
            };
        }
        return new String[] { "No special ability yet." };
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
}
