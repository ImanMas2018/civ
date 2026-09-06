package civ.view;

import civ.model.combat.BattleReport;
import civ.net.protocol.push.BattleReportPush;
import javax.swing.JOptionPane;
import java.awt.Component;

/** Shows a battle report when you were attacked while away (or a structure strike summary). */
public final class BattleReportDialog {

    private BattleReportDialog() {
    }

    public static void show(Component parent, BattleReportPush push) {
        if (push == null) {
            return;
        }
        if (push.isAsAttacker()
                && !push.getAttackRolls().isEmpty()
                && !push.getDefenceRolls().isEmpty()) {
            BattlePanel.showAnimated(parent, push.toReport(), () -> {
            });
            return;
        }
        if (push.isAsAttacker()) {
            JOptionPane.showMessageDialog(parent, push.getSummary(),
                    "Attack result", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BattleReport report = push.toReport();
        StringBuilder body = new StringBuilder();
        body.append("You were attacked while it was not your turn.\n\n");
        if (!report.getAttackRolls().isEmpty()) {
            body.append("Attacker dice: ").append(report.getAttackRolls()).append('\n');
            body.append("Your dice: ").append(report.getDefenceRolls()).append('\n');
            body.append("Hits on you: ").append(report.getHitsOnDefender()).append('\n');
            body.append("Hits on attacker: ").append(report.getHitsOnAttacker()).append('\n');
        }
        if (report.getStructureDamage() > 0) {
            body.append("Structure damage: ").append(report.getStructureDamage()).append('\n');
        }
        if (!report.getUnitsLost().isEmpty()) {
            body.append("Units lost: ").append(String.join(", ", report.getUnitsLost())).append('\n');
        }
        if (body.toString().endsWith("\n\n")) {
            body.append(push.getSummary());
        }
        JOptionPane.showMessageDialog(parent, body.toString(),
                "Battle report", JOptionPane.WARNING_MESSAGE);
    }
}
