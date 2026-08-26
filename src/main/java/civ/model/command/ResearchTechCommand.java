package civ.model.command;

import civ.model.Game;
import civ.model.Tech;

public class ResearchTechCommand implements Command {

    private final Tech tech;

    public ResearchTechCommand(Tech tech) {
        this.tech = tech;
    }

    @Override
    public String getLabel() {
        return "Researching " + tech.getLabel();
    }

    @Override
    public int getTurnsNeeded() {
        return tech.getTurns();
    }

    @Override
    public void payCost(Game game) {
        game.getEmpire().getStock().pay(tech.getWoodCost(), tech.getStoneCost(), tech.getIronCost());
    }

    @Override
    public void execute(Game game) {
        game.getEmpire().addTech(tech);
        game.addLog(tech.getLabel() + " finished.");
    }

    @Override
    public void cancel(Game game) {
        game.addLog(getLabel() + " cancelled. Resources are lost.");
    }

    public Tech getTech() {
        return tech;
    }
}
