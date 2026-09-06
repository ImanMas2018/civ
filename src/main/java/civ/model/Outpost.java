package civ.model;

public class Outpost extends Building {

    public Outpost(Hex hex) {
        super(BuildingType.OUTPOST, hex);
        setHealth(40, 40);
    }

    public Outpost(long id, long createdAt, Hex hex) {
        super(id, createdAt, BuildingType.OUTPOST, hex);
        setHealth(40, 40);
    }

    @Override
    public int outputPerTurn(Empire empire, GameMap map) {
        return 0;
    }
}
