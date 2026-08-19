package civ.model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of one dice fight. The view animates this; the model applies the hits. */
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

    public BattleReport(List<Integer> attackRolls, List<Integer> defenceRolls,
                        List<Pair> pairs, int hitsOnAttacker, int hitsOnDefender) {
        this.attackRolls = Collections.unmodifiableList(new ArrayList<>(attackRolls));
        this.defenceRolls = Collections.unmodifiableList(new ArrayList<>(defenceRolls));
        this.pairs = Collections.unmodifiableList(new ArrayList<>(pairs));
        this.hitsOnAttacker = hitsOnAttacker;
        this.hitsOnDefender = hitsOnDefender;
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
}
