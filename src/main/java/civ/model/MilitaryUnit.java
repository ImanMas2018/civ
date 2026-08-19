package civ.model;

/**
 * Shared combat stats. Disaster / body HP (later) is a different number —
 * dice fights only use {@link #getCombatHp()}.
 */
public abstract class MilitaryUnit extends Unit {

    private final int maxCombatHp;
    private final int attackPower;
    private final int attackRange;

    private int combatHp;

    protected MilitaryUnit(String typeName, int maxAp, int visionRadius,
                           int col, int row,
                           int combatHp, int attackPower, int attackRange) {
        super(typeName, maxAp, visionRadius, col, row);
        this.maxCombatHp = combatHp;
        this.combatHp = combatHp;
        this.attackPower = attackPower;
        this.attackRange = attackRange;
    }

    @Override
    public boolean isMilitary() {
        return true;
    }

    /** Barbarians and wild animals are not in the player's army. */
    public boolean isHostile() {
        return false;
    }

    public int getCombatHp() {
        return combatHp;
    }

    public int getMaxCombatHp() {
        return maxCombatHp;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public int getAttackRange() {
        return attackRange;
    }

    public void takeCombatHit() {
        combatHp--;
    }

    public boolean isDead() {
        return combatHp <= 0;
    }

    @Override
    public String describe() {
        return super.describe()
                + "  HP " + combatHp + "/" + maxCombatHp
                + "  atk " + attackPower
                + "  range " + attackRange;
    }
}
