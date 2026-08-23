package civ.model;

/** Disaster bear. Never damages buildings; fights with the normal dice system. */
public class Bear extends MilitaryUnit {

    public Bear(int col, int row) {
        super("Bear", 2, 1, col, row, 2, 35, 1);
        setBodyHp(120, 120);
    }

    @Override
    public boolean isHostile() {
        return true;
    }

    @Override
    public String getLetter() {
        return "K";
    }
}
