package civ.model.factory;

import civ.model.Game;
import civ.model.ResourceType;
import civ.model.Unit;
import civ.model.UnitBlueprint;

/**
 * One place that checks caps, then constructs a unit. Payment for queued training
 * happens in the Command; {@link #spawn} only builds the object.
 */
public class UnitFactory {

    private final Game game;

    public UnitFactory(Game game) {
        this.game = game;
    }

    public boolean canCreate(UnitBlueprint blueprint) {
        if (!blueprint.isMilitary() && game.getEmpire().isAtUnitCap()) {
            return false;
        }
        if (blueprint.isMilitary()
                && game.getEmpire().countMilitary() >= game.getEmpire().getMilitaryCap()) {
            return false;
        }
        if (blueprint.getRequiredLevel() > game.getEmpire().getTownHall().getLevel()) {
            return false;
        }
        if (blueprint == UnitBlueprint.CAVALRY && !game.hasMilitaryStable()) {
            return false;
        }
        return game.getEmpire().getStock().canPay(ResourceType.FOOD, blueprint.getFoodCost())
                && game.getEmpire().getStock().canPay(ResourceType.WOOD, blueprint.getWoodCost());
    }

    /** Constructs and registers the unit. Does not charge resources. */
    public Unit spawn(UnitBlueprint blueprint, int col, int row) {
        Unit unit = blueprint.create(col, row);
        game.addUnit(unit);
        return unit;
    }
}
