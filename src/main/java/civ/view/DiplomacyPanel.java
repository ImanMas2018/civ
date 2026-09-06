package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.Player;
import civ.model.diplomacy.DiplomaticState;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Lists every other player with diplomatic state and the legal actions. */
public class DiplomacyPanel extends JPanel {

    private final Game game;
    private final GameController controller;

    public DiplomacyPanel(Game game, GameController controller) {
        this.game = game;
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        rebuild();
    }

    public void rebuild() {
        removeAll();
        Player me = game.getViewpointPlayer();
        JLabel title = new JLabel("Diplomacy");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(8));

        for (Player other : game.getPlayers()) {
            if (other.getId() == me.getId()) {
                continue;
            }
            DiplomaticState state = game.getDiplomacy().between(me, other);
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel name = new JLabel(other.getName()
                    + (other.isAlive() ? "" : " (eliminated)")
                    + " — " + state.getLabel());
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(other.getColour().getAwt());
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(name);

            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton war = new JButton("Declare war");
            boolean canWar = other.isAlive() && state != DiplomaticState.ENEMY
                    && controller.isMyTurn();
            war.setEnabled(canWar);
            war.setToolTipText(canWar ? null
                    : !other.isAlive() ? "Player eliminated."
                    : state == DiplomaticState.ENEMY ? "Already at war."
                    : "It is not your turn.");
            war.addActionListener(e -> controller.declareWar(other.getId()));

            JButton ally = new JButton("Propose alliance");
            boolean canAlly = other.isAlive() && state != DiplomaticState.ALLIED
                    && controller.isMyTurn();
            ally.setEnabled(canAlly);
            ally.setToolTipText(canAlly ? null
                    : !other.isAlive() ? "Player eliminated."
                    : state == DiplomaticState.ALLIED ? "Already allied."
                    : "It is not your turn.");
            ally.addActionListener(e -> controller.proposeAlliance(other.getId()));

            JButton brk = new JButton("Break alliance");
            boolean canBreak = other.isAlive() && state == DiplomaticState.ALLIED
                    && controller.isMyTurn();
            brk.setEnabled(canBreak);
            brk.setToolTipText(canBreak ? null
                    : state != DiplomaticState.ALLIED ? "You are not allied."
                    : "It is not your turn.");
            brk.addActionListener(e -> controller.breakAlliance(other.getId()));

            buttons.add(war);
            buttons.add(Box.createHorizontalStrut(6));
            buttons.add(ally);
            buttons.add(Box.createHorizontalStrut(6));
            buttons.add(brk);
            row.add(buttons);
            row.add(Box.createVerticalStrut(10));
            add(row);
        }
        revalidate();
        repaint();
    }

    public static void showDialog(Component parent, Game game, GameController controller) {
        DiplomacyPanel panel = new DiplomacyPanel(game, controller);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(420, 280));
        javax.swing.JOptionPane.showMessageDialog(
                parent, scroll, "Diplomacy", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }
}
