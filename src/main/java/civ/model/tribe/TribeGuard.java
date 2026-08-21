package civ.model.tribe;

import civ.model.MilitaryUnit;

/** A tribe guard. Maps onto swordsman combat stats for dice fights. */
public class TribeGuard extends MilitaryUnit {

    private final Tribe tribe;

    public TribeGuard(Tribe tribe, int col, int row) {
        super("Tribe Guard", 2, 2, col, row, 1, 10, 1);
        this.tribe = tribe;
    }

    public Tribe getTribe() {
        return tribe;
    }

    @Override
    public boolean isHostile() {
        return tribe.getState().isHostile();
    }

    @Override
    public String getLetter() {
        return "G";
    }
}
