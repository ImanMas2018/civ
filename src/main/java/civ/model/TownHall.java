package civ.model;

/**
 * Starting building. The +1 food / +1 wood safeguard is applied in
 * {@link Empire#netRatePerTurn()} because it is two resources, not one.
 * The production queue arrives in Step 5.
 */
public class TownHall extends Building {

    public TownHall(Hex hex) {
        super(BuildingType.TOWN_HALL, hex);
    }

    @Override
    public int outputPerTurn(Empire empire) {
        return 0;
    }
}
