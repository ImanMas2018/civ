package civ.model;

/** Melee infantry. Unlocked from Town Hall level 1. */
public class Swordsman extends MilitaryUnit {

    public Swordsman(int col, int row) {
        super("Swordsman", 2, 2, col, row, 1, 10, 1);
    }

    @Override
    public String getLetter() {
        return "S";
    }
}
