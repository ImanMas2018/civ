package civ.model.command;

import civ.model.Game;
import civ.model.Hex;
import civ.model.ResourceType;
import civ.model.UnitBlueprint;

public class TrainUnitCommand implements Command {

    private final UnitBlueprint blueprint;

    public TrainUnitCommand(UnitBlueprint blueprint) {
        this.blueprint = blueprint;
    }

    @Override
    public String getLabel() {
        return "Training " + blueprint.getLabel();
    }

    @Override
    public int getTurnsNeeded() {
        return blueprint.getTurns();
    }

    @Override
    public void payCost(Game game) {
        game.getEmpire().getStock().add(ResourceType.FOOD, -blueprint.getFoodCost());
        game.getEmpire().getStock().add(ResourceType.WOOD, -blueprint.getWoodCost());
    }

    @Override
    public void execute(Game game) {
        Hex spawn = game.spawnHexFor(blueprint);
        game.getUnitFactory().spawn(blueprint, spawn.getCol(), spawn.getRow());
        game.addLog(blueprint.getLabel() + " is ready.");
    }

    @Override
    public void cancel(Game game) {
        game.addLog(getLabel() + " cancelled. Resources are lost.");
    }
}
