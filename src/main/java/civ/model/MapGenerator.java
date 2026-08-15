package civ.model;

import civ.util.HexGeometry;
import java.util.Random;

/** Fills a GameMap with terrain and resource deposits. Guarantees wood and food near the start. */
public class MapGenerator {

    private final Random random;

    public MapGenerator(long seed) {
        this.random = new Random(seed);
    }

    public void fill(GameMap map, int centreCol, int centreRow) {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                map.set(createHex(col, row));
            }
        }
        guaranteeForestNear(map, centreCol, centreRow);
        guaranteeFarmNear(map, centreCol, centreRow);
    }

    private Hex createHex(int col, int row) {
        int roll = random.nextInt(100);

        if (roll < 30) {
            return new Hex(col, row, Terrain.FOREST, ResourceType.WOOD, 60 + random.nextInt(40));
        }
        if (roll < 50) {
            boolean iron = random.nextInt(100) < 30;
            ResourceType deposit = iron ? ResourceType.IRON : ResourceType.STONE;
            return new Hex(col, row, Terrain.MOUNTAIN, deposit, 40 + random.nextInt(40));
        }
        if (roll < 75) {
            boolean food = random.nextInt(100) < 40;
            return new Hex(col, row, Terrain.GRASSLAND,
                    food ? ResourceType.FOOD : null,
                    food ? 80 + random.nextInt(40) : 0);
        }
        boolean animals = random.nextInt(100) < 30;
        return new Hex(col, row, Terrain.PLAINS,
                animals ? ResourceType.FOOD : null,
                animals ? 60 + random.nextInt(30) : 0);
    }

    /** A forest on the starting border, so a mill can be built before border expansion. */
    private void guaranteeForestNear(GameMap map, int centreCol, int centreRow) {
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() == Terrain.FOREST) {
                return;
            }
        }
        int[] spot = HexGeometry.neighbour(centreCol, centreRow, 0);
        if (map.inside(spot[0], spot[1])) {
            map.set(new Hex(spot[0], spot[1], Terrain.FOREST, ResourceType.WOOD, 80));
        }
    }

    /** A farmable grassland on the starting border, so food is not a dead end. */
    private void guaranteeFarmNear(GameMap map, int centreCol, int centreRow) {
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() == Terrain.GRASSLAND && hex.hasResource()
                    && hex.getDeposit() == ResourceType.FOOD) {
                return;
            }
        }
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() != Terrain.FOREST) {
                map.set(new Hex(hex.getCol(), hex.getRow(), Terrain.GRASSLAND, ResourceType.FOOD, 100));
                return;
            }
        }
    }
}
