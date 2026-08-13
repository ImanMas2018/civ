package civ.model;

/**
 * Starting building. The +1 food / +1 wood safeguard is applied in
 * {@link Empire#netRatePerTurn()} and again in {@link TurnEngine}.
 * Only one production order at a time.
 */
public class TownHall extends Building {

    private ProductionOrder order;

    public TownHall(Hex hex) {
        super(BuildingType.TOWN_HALL, hex);
    }

    public ProductionOrder getOrder() {
        return order;
    }

    public boolean isBusy() {
        return order != null;
    }

    public void setOrder(ProductionOrder order) {
        this.order = order;
    }

    public void clearOrder() {
        this.order = null;
    }

    @Override
    public int outputPerTurn(Empire empire) {
        return 0;
    }
}
