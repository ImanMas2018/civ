package civ.model;

/** Ranged military unit. Unlocked at Town Hall level 2. Combat comes in Step 3. */
public class Archer extends Unit {

    public Archer(int col, int row) {
        super("Archer", 2, 2, col, row);
    }

    @Override
    public String getLetter() {
        return "A";
    }

    @Override
    public boolean isMilitary() {
        return true;
    }
}
