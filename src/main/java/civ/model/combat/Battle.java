package civ.model.combat;

import civ.model.MilitaryUnit;
import java.util.ArrayList;
import java.util.List;

public class Battle {

    private final Dice dice;

    public Battle(Dice dice) {
        this.dice = dice;
    }

    public BattleReport resolve(int attackerDiceCount, int defenderDiceCount, int defenderBonus) {
        return resolve(attackerDiceCount, defenderDiceCount, 0, defenderBonus);
    }

    public BattleReport resolve(int attackerDiceCount, int defenderDiceCount,
                                int attackerBonus, int defenderBonus) {
        return compare(
                dice.roll(attackerDiceCount, attackerBonus),
                dice.roll(defenderDiceCount, defenderBonus));
    }

    /**
     * Pairs sorted rolls high→low. Ties go to the defender. Unpaired dice are ignored.
     */
    public static BattleReport compare(List<Integer> attackRolls, List<Integer> defenceRolls) {
        int hitsOnDefender = 0;
        int hitsOnAttacker = 0;
        List<BattleReport.Pair> pairs = new ArrayList<>();

        int pairCount = Math.min(attackRolls.size(), defenceRolls.size());
        for (int i = 0; i < pairCount; i++) {
            int attack = attackRolls.get(i);
            int defence = defenceRolls.get(i);
            boolean attackerWins = attack > defence;
            pairs.add(new BattleReport.Pair(attack, defence, attackerWins));
            if (attackerWins) {
                hitsOnDefender++;
            } else {
                hitsOnAttacker++;
            }
        }

        return new BattleReport(attackRolls, defenceRolls, pairs, hitsOnAttacker, hitsOnDefender);
    }

    public int damageToStructure(List<MilitaryUnit> attackers) {
        int total = 0;
        for (MilitaryUnit unit : attackers) {
            total += unit.getAttackPower();
            if (unit.isCombatBuffed()) {
                total += 5;
            }
        }
        return total;
    }
}
