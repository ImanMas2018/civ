package civ.model;

import civ.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;

public class TurnEngine {

    /**
     * Ends the current player's turn (production/upkeep for their empire),
     * hands control to the next player, and on a full round wrap runs tribes
     * plus TURN_ENDED (season/disasters).
     */
    public void endTurn(Game game) {
        Player acting = game.getCurrentPlayer();
        Empire empire = acting.getEmpire();
        int turnBefore = game.getTurn();

        game.expireItemEffects(acting);
        produceResources(game, empire);
        advanceTownHallQueue(game, empire);
        advanceApothecaryQueues(game, empire);
        payUpkeep(game, empire);
        eatFood(game, empire);
        checkStarvation(game, acting);

        refreshUnits(game, empire, acting.isStarving());
        game.getTradeTracker().clearTurn();

        game.advanceTurn();

        // Full round finished when advanceTurn wrapped and incremented the turn counter.
        if (game.getTurn() > turnBefore) {
            game.runTribeTurns();
            game.getBus().publish(GameEvent.TURN_ENDED, game);
        }
    }

    private void produceResources(Game game, Empire empire) {
        empire.getStock().add(ResourceType.FOOD, 1);
        empire.getStock().add(ResourceType.WOOD, 1);

        for (Building building : empire.getBuildings()) {
            ResourceType output = building.getType().getProduces();
            if (output == null) {
                continue;
            }

            int raw = building.outputPerTurn(empire, game.getMap());
            int amount = game.adjustProduction(building, raw);
            if (amount <= 0) {
                continue;
            }

            if (building.getType() == BuildingType.DOCK) {
                Adjacency.takeFish(building.getHex(), game.getMap(), amount);
                if (Adjacency.availableFish(building.getHex(), game.getMap()) <= 0
                        && building instanceof ProductionBuilding) {
                    ((ProductionBuilding) building).releaseAllWorkers();
                    game.addLog("The Dock has no more fish nearby.");
                }
            } else {
                int extracted = Math.min(
                        building.getHex().getDepositAmount(),
                        Math.max(0, raw - Adjacency.extraOutput(building, game.getMap())));
                if (extracted > 0) {
                    building.getHex().takeResource(extracted);
                }
            }
            empire.getStock().add(output, amount);

            if (building.getHex().isExhausted() && building instanceof ProductionBuilding
                    && building.getType() != BuildingType.DOCK) {
                ((ProductionBuilding) building).releaseAllWorkers();
                game.addLog("The " + building.getType().getLabel() + " ran out of resources.");
            }
        }

        int farmBonus = Adjacency.farmPairs(empire, game.getMap());
        if (farmBonus > 0) {
            empire.getStock().add(ResourceType.FOOD, farmBonus);
        }
        int allyFood = Adjacency.allyFarmBonus(empire, game.getTribes());
        if (allyFood > 0) {
            empire.getStock().add(ResourceType.FOOD, allyFood);
        }
        int allyStone = Adjacency.allyMineBonus(empire, game.getTribes());
        if (allyStone > 0) {
            empire.getStock().add(ResourceType.STONE, allyStone);
        }
    }

    private void advanceTownHallQueue(Game game, Empire empire) {
        TownHall hall = empire.getTownHall();
        if (hall != null) {
            hall.tick(game);
        }
        // Also tick any additional town halls' queues if present
        for (Building building : empire.getBuildings()) {
            if (building instanceof TownHall && building != hall) {
                ((TownHall) building).tick(game);
            }
        }
    }

    private void advanceApothecaryQueues(Game game, Empire empire) {
        for (Building building : empire.getBuildings()) {
            if (building instanceof Apothecary) {
                ((Apothecary) building).tick(game);
            }
        }
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

    private void checkStarvation(Game game, Player player) {
        boolean starving = player.getEmpire().getStock().get(ResourceType.FOOD) < 0;
        if (starving && !player.isStarving()) {
            game.addLog("STARVATION! Units are weakened.");
        }
        player.setStarving(starving);
    }

    private void refreshUnits(Game game, Empire empire, boolean starving) {
        int penalty = starving ? 1 : 0;
        penalty += empire.getHappiness().apPenalty();
        for (Unit unit : empire.getUnits()) {
            unit.refresh(penalty);
        }
        for (MilitaryUnit hostile : game.getHostiles()) {
            hostile.refresh(0);
        }
    }
}
