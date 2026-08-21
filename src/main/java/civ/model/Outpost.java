package civ.model;

/** Former tribe camp claimed by the player. */
public class Outpost extends Building {

    public Outpost(Hex hex) {
        super(BuildingType.OUTPOST, hex);
        setHealth(40, 40);
    }

    @Override
    public int outputPerTurn(Empire empire, GameMap map) {
        return 0;
    }
}
