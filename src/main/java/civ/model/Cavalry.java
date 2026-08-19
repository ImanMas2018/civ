package civ.model;

/** Fast melee unit. Needs Town Hall level 2 and a Military Stable. */
public class Cavalry extends MilitaryUnit {

    public Cavalry(int col, int row) {
        super("Cavalry", 4, 2, col, row, 2, 8, 1);
    }

    @Override
    public String getLetter() {
        return "C";
    }
}
