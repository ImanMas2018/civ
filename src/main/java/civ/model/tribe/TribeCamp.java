package civ.model.tribe;

import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Empire;
import civ.model.GameMap;
import civ.model.Hex;

public class TribeCamp extends Building {

    private final Tribe tribe;

    public TribeCamp(Tribe tribe, Hex hex) {
        super(BuildingType.TRIBE_CAMP, hex);
        this.tribe = tribe;
        setHealth(tribe.getType().getCampHp(), tribe.getType().getCampHp());
    }

    public TribeCamp(long id, long createdAt, Tribe tribe, Hex hex) {
        super(id, createdAt, BuildingType.TRIBE_CAMP, hex);
        this.tribe = tribe;
        setHealth(tribe.getType().getCampHp(), tribe.getType().getCampHp());
    }

    public Tribe getTribe() {
        return tribe;
    }

    @Override
    public int outputPerTurn(Empire empire, GameMap map) {
        return 0;
    }
}
