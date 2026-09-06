package civ.model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleReport {

    public static final class Pair {
        private final int attack;
        private final int defence;
        private final boolean attackerWins;

        public Pair(int attack, int defence, boolean attackerWins) {
            this.attack = attack;
            this.defence = defence;
            this.attackerWins = attackerWins;
        }

        public int getAttack() {
            return attack;
        }

        public int getDefence() {
            return defence;
        }

        public boolean attackerWins() {
            return attackerWins;
        }

        public String describe() {
            return attack + " vs " + defence + " — "
                    + (attackerWins ? "attacker" : "defender");
        }
    }

    private final List<Integer> attackRolls;
    private final List<Integer> defenceRolls;
    private final List<Pair> pairs;
    private final int hitsOnAttacker;
    private final int hitsOnDefender;
    private int structureDamage;
    private final List<String> unitsLost = new ArrayList<>();
    private String targetLabel = "";

    public BattleReport(List<Integer> attackRolls, List<Integer> defenceRolls,
                        List<Pair> pairs, int hitsOnAttacker, int hitsOnDefender) {
        this.attackRolls = Collections.unmodifiableList(new ArrayList<>(attackRolls));
        this.defenceRolls = Collections.unmodifiableList(new ArrayList<>(defenceRolls));
        this.pairs = Collections.unmodifiableList(new ArrayList<>(pairs));
        this.hitsOnAttacker = hitsOnAttacker;
        this.hitsOnDefender = hitsOnDefender;
    }

    /** Structure-only strike (no dice). */
    public static BattleReport structureOnly(int damage, String targetLabel) {
        BattleReport report = new BattleReport(
                List.of(), List.of(), List.of(), 0, 0);
        report.structureDamage = damage;
        report.targetLabel = targetLabel == null ? "" : targetLabel;
        return report;
    }

    public List<Integer> getAttackRolls() {
        return attackRolls;
    }

    public List<Integer> getDefenceRolls() {
        return defenceRolls;
    }

    public List<Pair> getPairs() {
        return pairs;
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

    public void setStructureDamage(int structureDamage) {
        this.structureDamage = structureDamage;
    }

    public List<String> getUnitsLost() {
        return unitsLost;
    }

    public void noteUnitLost(String description) {
        if (description != null && !description.isEmpty()) {
            unitsLost.add(description);
        }
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel == null ? "" : targetLabel;
    }

    public String summary() {
        StringBuilder text = new StringBuilder();
        if (!attackRolls.isEmpty() || !defenceRolls.isEmpty()) {
            text.append("Hits on defender: ").append(hitsOnDefender)
                    .append(", hits on attacker: ").append(hitsOnAttacker);
        }
        if (structureDamage > 0) {
            if (text.length() > 0) {
                text.append(". ");
            }
            text.append("Structure damage: ").append(structureDamage);
            if (!targetLabel.isEmpty()) {
                text.append(" to ").append(targetLabel);
            }
        }
        if (!unitsLost.isEmpty()) {
            if (text.length() > 0) {
                text.append(". ");
            }
            text.append("Lost: ").append(String.join(", ", unitsLost));
        }
        return text.length() == 0 ? "Battle resolved." : text.toString();
    }
}
