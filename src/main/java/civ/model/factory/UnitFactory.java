package civ.model.factory;

import civ.model.Game;
import civ.model.ResourceType;
import civ.model.TownHall;
import civ.model.Unit;
import civ.model.UnitBlueprint;

public class UnitFactory {

    private final Game game;

    public UnitFactory(Game game) {
        this.game = game;
    }

    public boolean canCreate(UnitBlueprint blueprint) {
        TownHall hall = game.getEmpire().getTownHall();
        if (hall == null) {
            return false;
        }
        if (!blueprint.isMilitary() && game.getEmpire().isAtUnitCap()) {
            return false;
        }
        if (blueprint.isMilitary()
                && game.getEmpire().countMilitary() >= game.getEmpire().getMilitaryCap()) {
            return false;
        }
        if (blueprint.getRequiredLevel() > hall.getLevel()) {
            return false;
        }
        if (blueprint == UnitBlueprint.CAVALRY && !game.hasMilitaryStable()) {
            return false;
        }
        return game.getEmpire().getStock().canPay(ResourceType.FOOD, blueprint.getFoodCost())
                && game.getEmpire().getStock().canPay(ResourceType.WOOD, blueprint.getWoodCost());
    }

    /** Constructs and registers the unit for the current player. Does not charge resources. */
    public Unit spawn(UnitBlueprint blueprint, int col, int row) {
        Unit unit = blueprint.create(col, row);
        unit.setOwnerId(game.getCurrentPlayer().getId());
        game.addUnit(unit);
        return unit;
    }
}
