package civ.model;

/** A defensive wall sitting on the edge between two hexes. */
public class Wall {

    private int hp = 50;
    private final int maxHp = 50;

    public Wall() {
    }

    public Wall(int hp) {
        this.hp = hp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(maxHp, hp));
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }
}
