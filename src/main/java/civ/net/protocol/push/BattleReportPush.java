package civ.net.protocol.push;

import civ.model.combat.BattleReport;
import civ.net.protocol.Broadcast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleReportPush extends Broadcast {

    public static final String TYPE = "battle_report";

    private final boolean asAttacker;
    private final List<Integer> attackRolls;
    private final List<Integer> defenceRolls;
    private final int hitsOnAttacker;
    private final int hitsOnDefender;
    private final int structureDamage;
    private final List<String> unitsLost;
    private final String summary;

    public BattleReportPush(BattleReport report, boolean asAttacker) {
        super(TYPE);
        this.asAttacker = asAttacker;
        this.attackRolls = new ArrayList<>(report.getAttackRolls());
        this.defenceRolls = new ArrayList<>(report.getDefenceRolls());
        this.hitsOnAttacker = report.getHitsOnAttacker();
        this.hitsOnDefender = report.getHitsOnDefender();
        this.structureDamage = report.getStructureDamage();
        this.unitsLost = new ArrayList<>(report.getUnitsLost());
        this.summary = report.summary();
    }

    public boolean isAsAttacker() {
        return asAttacker;
    }

    public List<Integer> getAttackRolls() {
        return Collections.unmodifiableList(attackRolls);
    }

    public List<Integer> getDefenceRolls() {
        return Collections.unmodifiableList(defenceRolls);
    }

    public int getHitsOnAttacker() {
        return hitsOnAttacker;
    }

    public int getHitsOnDefender() {
        return hitsOnDefender;
    }

    public int getStructureDamage() {
        return structureDamage;
    }

    public List<String> getUnitsLost() {
        return Collections.unmodifiableList(unitsLost);
    }

    public String getSummary() {
        return summary;
    }

    public BattleReport toReport() {
        List<BattleReport.Pair> pairs = new ArrayList<>();
        int pairCount = Math.min(attackRolls.size(), defenceRolls.size());
        for (int i = 0; i < pairCount; i++) {
            int attack = attackRolls.get(i);
            int defence = defenceRolls.get(i);
            pairs.add(new BattleReport.Pair(attack, defence, attack > defence));
        }
        BattleReport report = new BattleReport(
                attackRolls, defenceRolls, pairs, hitsOnAttacker, hitsOnDefender);
        report.setStructureDamage(structureDamage);
        for (String lost : unitsLost) {
            report.noteUnitLost(lost);
        }
        return report;
    }
}
