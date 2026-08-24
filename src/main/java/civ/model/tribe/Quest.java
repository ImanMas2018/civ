package civ.model.tribe;

import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.ResourceType;
import civ.model.Swordsman;
import civ.util.HexGeometry;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/** One short-term goal offered by a tribe. */
public class Quest {

    private final String title;
    private final String description;
    private final int deadlineTurns;
    private final int relationReward;
    private final QuestKind kind;

    private QuestStatus status = QuestStatus.AVAILABLE;
    private int turnsLeft;
    private int progress;

    public enum QuestKind {
        FARMER_SUPPLY,
        TRADER_ROAD,
        WARRIOR_KILLS,
        MOUNTAIN_TOOLS,
        COASTAL_DOCK
    }

    public Quest(String title, String description, int deadlineTurns,
                 int relationReward, QuestKind kind) {
        this.title = title;
        this.description = description;
        this.deadlineTurns = deadlineTurns;
        this.relationReward = relationReward;
        this.kind = kind;
        this.turnsLeft = deadlineTurns;
    }

    public static Quest forType(TribeType type) {
        switch (type) {
            case FARMER:
                return new Quest("Help the granary",
                        "Pay 20 wood and 10 stone to the tribe.",
                        5, 15, QuestKind.FARMER_SUPPLY);
            case TRADER:
                return new Quest("Trade route",
                        "Build a continuous road from one of your buildings to a hex next to their camp.",
                        10, 20, QuestKind.TRADER_ROAD);
            case WARRIOR:
                return new Quest("Military aid",
                        "Defeat 2 enemy or barbarian units within 5 hexes of their camp.",
                        8, 20, QuestKind.WARRIOR_KILLS);
            case MOUNTAIN:
                return new Quest("Mining tools",
                        "Pay 15 wood and 10 iron to the tribe.",
                        6, 15, QuestKind.MOUNTAIN_TOOLS);
            case COASTAL:
                return new Quest("Coastal growth",
                        "Build a Dock within 4 hexes of their camp.",
                        10, 15, QuestKind.COASTAL_DOCK);
            default:
                throw new IllegalStateException("unknown tribe " + type);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDeadlineTurns() {
        return deadlineTurns;
    }

    public int getRelationReward() {
        return relationReward;
    }

    public QuestKind getKind() {
        return kind;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public int getTurnsLeft() {
        return turnsLeft;
    }

    public int getProgress() {
        return progress;
    }

    public void accept() {
        status = QuestStatus.ACTIVE;
        turnsLeft = deadlineTurns;
        progress = 0;
    }

    public void restore(QuestStatus status, int turnsLeft, int progress) {
        this.status = status;
        this.turnsLeft = turnsLeft;
        this.progress = progress;
    }

    public void cancel() {
        status = QuestStatus.CANCELLED;
    }

    public void tickDeadline() {
        if (status != QuestStatus.ACTIVE && status != QuestStatus.READY) {
            return;
        }
        turnsLeft--;
        if (turnsLeft <= 0 && status == QuestStatus.ACTIVE) {
            status = QuestStatus.FAILED;
        }
    }

    public void refreshReady(Game game, Tribe tribe) {
        if (status != QuestStatus.ACTIVE && status != QuestStatus.READY) {
            return;
        }
        if (isConditionMet(game, tribe)) {
            status = QuestStatus.READY;
        } else {
            status = QuestStatus.ACTIVE;
        }
    }

    public boolean isConditionMet(Game game, Tribe tribe) {
        switch (kind) {
            case FARMER_SUPPLY:
                return game.getEmpire().getStock().canPay(ResourceType.WOOD, 20)
                        && game.getEmpire().getStock().canPay(ResourceType.STONE, 10);
            case MOUNTAIN_TOOLS:
                return game.getEmpire().getStock().canPay(ResourceType.WOOD, 15)
                        && game.getEmpire().getStock().canPay(ResourceType.IRON, 10);
            case TRADER_ROAD:
                return hasRoadToCamp(game, tribe);
            case WARRIOR_KILLS:
                return progress >= 2;
            case COASTAL_DOCK:
                return hasNearbyDock(game, tribe);
            default:
                return false;
        }
    }

    public boolean canDeliver(Game game, Tribe tribe) {
        if (status != QuestStatus.ACTIVE && status != QuestStatus.READY) {
            return false;
        }
        if (!isConditionMet(game, tribe)) {
            return false;
        }
        if (kind == QuestKind.FARMER_SUPPLY) {
            return roomFor(game, ResourceType.FOOD, 30);
        }
        if (kind == QuestKind.MOUNTAIN_TOOLS) {
            return roomFor(game, ResourceType.STONE, 20);
        }
        if (kind == QuestKind.COASTAL_DOCK) {
            return roomFor(game, ResourceType.FOOD, 30);
        }
        return true;
    }

    public String deliverBlockedReason(Game game) {
        if (status != QuestStatus.ACTIVE && status != QuestStatus.READY) {
            return "Take the quest first.";
        }
        if (kind == QuestKind.TRADER_ROAD) {
            return "Build a continuous road from one of your buildings to a hex next to their camp.";
        }
        if (kind == QuestKind.WARRIOR_KILLS) {
            return "Defeat 2 enemies within 5 hexes of their camp (progress: " + progress + "/2).";
        }
        if (kind == QuestKind.COASTAL_DOCK) {
            return "Build a Dock within 4 hexes of their camp.";
        }
        if (kind == QuestKind.FARMER_SUPPLY) {
            if (!roomFor(game, ResourceType.FOOD, 30)) {
                return "Not enough stockpile room for the food reward.";
            }
            return "Needs 20 wood and 10 stone in stock to deliver.";
        }
        if (kind == QuestKind.MOUNTAIN_TOOLS) {
            if (!roomFor(game, ResourceType.STONE, 20)) {
                return "Not enough stockpile room for the stone reward.";
            }
            return "Needs 15 wood and 10 iron in stock to deliver.";
        }
        return "Quest condition is not met yet.";
    }

    public void deliver(Game game, Tribe tribe) {
        if (!canDeliver(game, tribe)) {
            return;
        }
        switch (kind) {
            case FARMER_SUPPLY:
                game.getEmpire().getStock().add(ResourceType.WOOD, -20);
                game.getEmpire().getStock().add(ResourceType.STONE, -10);
                game.getEmpire().getStock().add(ResourceType.FOOD, 30);
                break;
            case MOUNTAIN_TOOLS:
                game.getEmpire().getStock().add(ResourceType.WOOD, -15);
                game.getEmpire().getStock().add(ResourceType.IRON, -10);
                game.getEmpire().getStock().add(ResourceType.STONE, 20);
                break;
            case TRADER_ROAD:
                tribe.addTradeBonusPercent(10);
                break;
            case WARRIOR_KILLS:
                Hex camp = tribe.getCampHex();
                for (int i = 0; i < 3; i++) {
                    game.addUnit(new Swordsman(camp.getCol(), camp.getRow()));
                }
                break;
            case COASTAL_DOCK:
                game.getEmpire().getStock().add(ResourceType.FOOD, 30);
                game.setNextDockHalfPrice(true);
                break;
            default:
                break;
        }
        status = QuestStatus.COMPLETED;
        tribe.changeRelation(relationReward, game.getBus());
        game.addLog("Quest \"" + title + "\" delivered to the " + tribe.getName() + ".");
    }

    public void noteKillNearCamp(Tribe tribe, Hex killHex) {
        if (kind != QuestKind.WARRIOR_KILLS || status != QuestStatus.ACTIVE) {
            return;
        }
        Hex camp = tribe.getCampHex();
        int d = HexGeometry.distance(camp.getCol(), camp.getRow(), killHex.getCol(), killHex.getRow());
        if (d <= 5) {
            progress++;
        }
    }

    private static boolean roomFor(Game game, ResourceType type, int amount) {
        return game.getEmpire().getStock().get(type) + amount
                <= game.getEmpire().getStock().getCapacity();
    }

    private static boolean hasNearbyDock(Game game, Tribe tribe) {
        Hex camp = tribe.getCampHex();
        for (Building building : game.getEmpire().getBuildings()) {
            if (building.getType() != BuildingType.DOCK) {
                continue;
            }
            Hex hex = building.getHex();
            if (HexGeometry.distance(camp.getCol(), camp.getRow(), hex.getCol(), hex.getRow()) <= 4) {
                return true;
            }
        }
        return false;
    }

    /**
     * Continuous road from a player building to a hex next to the camp.
     * The camp neighbour must itself have a road.
     */
    private static boolean hasRoadToCamp(Game game, Tribe tribe) {
        Hex camp = tribe.getCampHex();
        Set<Hex> goals = new HashSet<>();
        for (Hex neighbour : game.getMap().neighbours(camp)) {
            if (neighbour.hasRoad() && neighbour.getTerrain().isLand()) {
                goals.add(neighbour);
            }
        }
        if (goals.isEmpty()) {
            return false;
        }

        Queue<Hex> queue = new ArrayDeque<>();
        Set<Hex> seen = new HashSet<>();
        for (Building building : game.getEmpire().getBuildings()) {
            Hex hex = building.getHex();
            if (hex == null || !hex.getTerrain().isLand()) {
                continue;
            }
            queue.add(hex);
            seen.add(hex);
        }
        while (!queue.isEmpty()) {
            Hex here = queue.poll();
            if (goals.contains(here)) {
                return true;
            }
            for (Hex next : game.getMap().neighbours(here)) {
                if (seen.contains(next) || !next.hasRoad() || !next.getTerrain().isLand()) {
                    continue;
                }
                seen.add(next);
                queue.add(next);
            }
        }
        return false;
    }
}
