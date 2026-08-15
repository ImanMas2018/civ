package civ.model.command;

import civ.model.Game;
import civ.model.Hex;
import civ.model.ResourceType;
import civ.model.UnitBlueprint;
import java.util.List;

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
        Hex home = game.getEmpire().getTownHall().getHex();
        List<Hex> ring = game.getMap().neighbours(home);
        Hex spawn = ring.isEmpty() ? home : ring.get(0);
        game.getUnitFactory().spawn(blueprint, spawn.getCol(), spawn.getRow());
        game.addLog(blueprint.getLabel() + " is ready.");
    }

    @Override
    public void cancel(Game game) {
        game.addLog(getLabel() + " cancelled. Resources are lost.");
    }
}
