package civ.model;

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
        setBodyHp(80, 80);
    }

    protected MilitaryUnit(long id, long createdAt, String typeName, int maxAp, int visionRadius,
                           int col, int row,
                           int combatHp, int attackPower, int attackRange) {
        super(id, createdAt, typeName, maxAp, visionRadius, col, row);
        this.maxCombatHp = combatHp;
        this.combatHp = combatHp;
        this.attackPower = attackPower;
        this.attackRange = attackRange;
        setBodyHp(80, 80);
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

    public void setCombatHp(int combatHp) {
        this.combatHp = Math.max(0, Math.min(maxCombatHp, combatHp));
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
