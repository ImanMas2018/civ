package civ.model.command;

import civ.model.Game;
import civ.model.TownHallLevel;

public class UpgradeTownHallCommand implements Command {

    private final TownHallLevel target;

    public UpgradeTownHallCommand(TownHallLevel target) {
        this.target = target;
    }

    @Override
    public String getLabel() {
        return "Upgrading to " + target.getLabel();
    }

    @Override
    public int getTurnsNeeded() {
        return target.getTurns();
    }

    @Override
    public void payCost(Game game) {
        game.getEmpire().getStock().pay(
                target.getWoodCost(), target.getStoneCost(), target.getIronCost());
    }

    @Override
    public void execute(Game game) {
        game.getEmpire().getTownHall().promote(game);
    }

    @Override
    public void cancel(Game game) {
        game.addLog(getLabel() + " cancelled. Resources are lost.");
    }
}
