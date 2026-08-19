package civ.model;

/** Ranged military unit. Unlocked at Town Hall level 2. */
public class Archer extends MilitaryUnit {

    public Archer(int col, int row) {
        super("Archer", 2, 2, col, row, 1, 6, 2);
    }

    @Override
    public String getLetter() {
        return "A";
    }
}
