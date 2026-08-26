package civ.model;

public final class Adjacency {

    private Adjacency() {
    }

    /** Each shared farm-farm edge counts once: two farms +1, three in a row +2. */
    public static int farmPairs(Empire empire, GameMap map) {
        int pairs = 0;
        for (Building building : empire.getBuildings()) {
            if (building.getType() != BuildingType.FARM) {
                continue;
            }
            Hex hex = building.getHex();
            for (Hex neighbour : map.neighbours(hex)) {
                Building other = neighbour.getBuilding();
                if (other == null || other.getType() != BuildingType.FARM) {
                    continue;
                }
                if (comesFirst(hex, neighbour)) {
                    pairs++;
                }
            }
        }
        return pairs;
    }

    public static int extraOutput(Building building, GameMap map) {
        BuildingType type = building.getType();
        if (type == BuildingType.LUMBER_MILL && touchesSea(building.getHex(), map)) {
            return 2;
        }
        if ((type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE)
                && mountainNeighbours(building.getHex(), map) >= 2) {
            return 1;
        }
        return 0;
    }

    public static int availableFish(Hex dock, GameMap map) {
        int fish = 0;
        for (Hex neighbour : map.neighbours(dock)) {
            if (neighbour.getTerrain().isSea()
                    && neighbour.getDeposit() == ResourceType.FOOD) {
                fish += neighbour.getDepositAmount();
            }
        }
        return fish;
    }

    public static void takeFish(Hex dock, GameMap map, int amount) {
        int left = amount;
        for (Hex neighbour : map.neighbours(dock)) {
            if (left <= 0) {
                return;
            }
            if (neighbour.getTerrain().isSea()
                    && neighbour.getDeposit() == ResourceType.FOOD
                    && neighbour.getDepositAmount() > 0) {
                int take = Math.min(left, neighbour.getDepositAmount());
                neighbour.takeResource(take);
                left -= take;
            }
        }
    }

    public static int allyFarmBonus(Empire empire, java.util.List<civ.model.tribe.Tribe> tribes) {
        if (tribes == null) {
            return 0;
        }
        boolean allied = false;
        for (civ.model.tribe.Tribe tribe : tribes) {
            if (tribe.isAllied() && tribe.getType() == civ.model.tribe.TribeType.FARMER) {
                allied = true;
                break;
            }
        }
        if (!allied) {
            return 0;
        }
        int farms = 0;
        for (Building building : empire.getBuildings()) {
            if (building.getType() == BuildingType.FARM) {
                farms++;
            }
        }
        return farms;
    }

    /** Allied Mountain tribe: +1 stone per Stone Mine. */
    public static int allyMineBonus(Empire empire, java.util.List<civ.model.tribe.Tribe> tribes) {
        if (tribes == null) {
            return 0;
        }
        boolean allied = false;
        for (civ.model.tribe.Tribe tribe : tribes) {
            if (tribe.isAllied() && tribe.getType() == civ.model.tribe.TribeType.MOUNTAIN) {
                allied = true;
                break;
            }
        }
        if (!allied) {
            return 0;
        }
        int mines = 0;
        for (Building building : empire.getBuildings()) {
            if (building.getType() == BuildingType.STONE_MINE) {
                mines++;
            }
        }
        return mines;
    }

    private static boolean touchesSea(Hex hex, GameMap map) {
        for (Hex neighbour : map.neighbours(hex)) {
            if (neighbour.getTerrain().isSea()) {
                return true;
            }
        }
        return false;
    }

    private static int mountainNeighbours(Hex hex, GameMap map) {
        int n = 0;
        for (Hex neighbour : map.neighbours(hex)) {
            if (neighbour.getTerrain() == Terrain.MOUNTAIN
                    || neighbour.getTerrain().isMountainRange()) {
                n++;
            }
        }
        return n;
    }

    private static boolean comesFirst(Hex a, Hex b) {
        return a.getRow() < b.getRow()
                || (a.getRow() == b.getRow() && a.getCol() <= b.getCol());
    }
}
