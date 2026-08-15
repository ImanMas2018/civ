package civ.model;

import civ.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete End Turn sequence, in the order the specification lists:
 * produce → advance queue → pay upkeep → eat food → check starvation → next turn → refresh AP.
 */
public class TurnEngine {

    public void endTurn(Game game) {
        Empire empire = game.getEmpire();

        produceResources(game, empire);
        advanceTownHallQueue(game, empire);
        payUpkeep(game, empire);
        eatFood(game, empire);
        checkStarvation(game, empire);

        game.nextTurn();
        refreshUnits(game, empire);

        // Future systems (tribes, seasons, disasters, autosave) subscribe here.
        // Do not call them from this class — that would invert the dependency.
        game.getBus().publish(GameEvent.TURN_ENDED, game);
    }

    private void produceResources(Game game, Empire empire) {
        empire.getStock().add(ResourceType.FOOD, 1);
        empire.getStock().add(ResourceType.WOOD, 1);

        for (Building building : empire.getBuildings()) {
            ResourceType output = building.getType().getProduces();
            if (output == null) {
                continue;
            }

            int amount = building.outputPerTurn(empire);
            if (amount <= 0) {
                continue;
            }

            building.getHex().takeResource(amount);
            empire.getStock().add(output, amount);

            if (building.getHex().isExhausted() && building instanceof ProductionBuilding) {
                ((ProductionBuilding) building).releaseAllWorkers();
                game.addLog("The " + building.getType().getLabel() + " ran out of resources.");
            }
        }
    }

    private void advanceTownHallQueue(Game game, Empire empire) {
        empire.getTownHall().tick(game);
    }

    private void payUpkeep(Game game, Empire empire) {
        List<Building> collapsed = new ArrayList<>();

        for (Building building : empire.getBuildings()) {
            ResourceType resource = building.getType().getUpkeepResource();
            int amount = building.getType().getUpkeepAmount();
            if (resource == null || amount == 0) {
                continue;
            }

            if (empire.getStock().canPay(resource, amount)) {
                empire.getStock().add(resource, -amount);
                building.markPaid();
            } else {
                building.markUnpaid();
                game.addLog("Could not pay upkeep for the " + building.getType().getLabel()
                        + " (" + building.getUnpaidTurns() + "/3).");
                if (building.isCollapsed()) {
                    collapsed.add(building);
                }
            }
        }

        for (Building building : collapsed) {
            if (building instanceof ProductionBuilding) {
                ((ProductionBuilding) building).releaseAllWorkers();
            }
            building.getHex().setBuilding(null);
            empire.getBuildings().remove(building);
            game.addLog("The " + building.getType().getLabel() + " collapsed from neglect.");
        }
    }

    private void eatFood(Game game, Empire empire) {
        int eaten = empire.getUnits().size();
        empire.getStock().add(ResourceType.FOOD, -eaten);
    }

    private void checkStarvation(Game game, Empire empire) {
        boolean starving = empire.getStock().get(ResourceType.FOOD) < 0;
        if (starving && !game.isStarving()) {
            game.addLog("STARVATION! Units are weakened.");
        }
        game.setStarving(starving);
    }

    private void refreshUnits(Game game, Empire empire) {
        int penalty = game.isStarving() ? 1 : 0;
        for (Unit unit : empire.getUnits()) {
            unit.refresh(penalty);
        }
    }
}
