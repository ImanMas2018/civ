package civ.view;

import civ.model.combat.BattleReport;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shows both sides' dice rolling, then the pairing and who won each pair.
 * The model has already decided the numbers; this only replays them.
 */
public final class BattlePanel {

    private static final Color BG = new Color(28, 32, 42);
    private static final Color TEXT = new Color(230, 235, 245);
    private static final Color ATTACK = new Color(80, 160, 255);
    private static final Color DEFENCE = new Color(220, 90, 90);
    private static final Color WIN = new Color(120, 210, 130);

    private BattlePanel() {
    }

    public static void showAnimated(Component parent, BattleReport report, Runnable onDone) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Battle", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel();
        root.setBackground(BG);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = heading("Dice battle");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(8));

        JLabel attackCaption = caption("Attacker");
        attackCaption.setForeground(ATTACK);
        attackCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(attackCaption);
        JPanel attackRow = diceRow(report.getAttackRolls().size());
        attackRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(attackRow);
        root.add(Box.createVerticalStrut(10));

        JLabel defenceCaption = caption("Defender");
        defenceCaption.setForeground(DEFENCE);
        defenceCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(defenceCaption);
        JPanel defenceRow = diceRow(report.getDefenceRolls().size());
        defenceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(defenceRow);
        root.add(Box.createVerticalStrut(12));

        JPanel pairs = new JPanel();
        pairs.setOpaque(false);
        pairs.setLayout(new BoxLayout(pairs, BoxLayout.Y_AXIS));
        pairs.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(pairs);

        JLabel summary = body("");
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(Box.createVerticalStrut(8));
        root.add(summary);

        JButton continueButton = new JButton("Continue");
        continueButton.setEnabled(false);
        continueButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(Box.createVerticalStrut(12));
        root.add(continueButton);

        boolean[] finished = {false};
        Runnable finish = () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            dialog.dispose();
            if (onDone != null) {
                onDone.run();
            }
        };
        continueButton.addActionListener(e -> finish.run());
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                finish.run();
            }
        });

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);

        List<JLabel> attackDice = labelsIn(attackRow);
        List<JLabel> defenceDice = labelsIn(defenceRow);
        Random flicker = new Random();

        Timer roll = new Timer(70, null);
        int[] ticks = {0};
        roll.addActionListener(e -> {
            ticks[0]++;
            flickerDice(attackDice, flicker);
            flickerDice(defenceDice, flicker);
            if (ticks[0] >= 14) {
                roll.stop();
                settle(attackDice, report.getAttackRolls());
                settle(defenceDice, report.getDefenceRolls());
                for (BattleReport.Pair pair : report.getPairs()) {
                    JLabel line = body(pair.describe());
                    line.setForeground(pair.attackerWins() ? ATTACK : DEFENCE);
                    line.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pairs.add(line);
                }
                if (report.getPairs().isEmpty()) {
                    JLabel none = body("No paired dice.");
                    none.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pairs.add(none);
                }
                summary.setText("Hits on defender: " + report.getHitsOnDefender()
                        + "    Hits on attacker: " + report.getHitsOnAttacker());
                summary.setForeground(WIN);
                continueButton.setEnabled(true);
                dialog.pack();
            }
        });
        roll.start();
        dialog.setVisible(true);
    }

    private static JPanel diceRow(int count) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        if (count == 0) {
            row.add(body("(no dice)"));
            return row;
        }
        for (int i = 0; i < count; i++) {
            JLabel die = new JLabel("?");
            die.setFont(new Font("SansSerif", Font.BOLD, 28));
            die.setForeground(TEXT);
            die.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(90, 100, 120)),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            die.setOpaque(true);
            die.setBackground(new Color(40, 46, 58));
            row.add(die);
            row.add(Box.createHorizontalStrut(8));
        }
        return row;
    }

    private static List<JLabel> labelsIn(JPanel row) {
        List<JLabel> labels = new ArrayList<>();
        for (Component child : row.getComponents()) {
            if (child instanceof JLabel && "?".equals(((JLabel) child).getText())) {
                labels.add((JLabel) child);
            }
        }
        return labels;
    }

    private static void flickerDice(List<JLabel> dice, Random random) {
        for (JLabel die : dice) {
            die.setText(String.valueOf(1 + random.nextInt(6)));
        }
    }

    private static void settle(List<JLabel> dice, List<Integer> values) {
        for (int i = 0; i < dice.size() && i < values.size(); i++) {
            dice.get(i).setText(String.valueOf(values.get(i)));
        }
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(TEXT);
        return label;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    private static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(TEXT);
        return label;
    }
}
