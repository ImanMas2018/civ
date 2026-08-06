package civ.model;

import civ.util.HexGeometry;
import java.util.Random;

/** Fills a GameMap with terrain and resource deposits. Guarantees wood near the start. */
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

    private void guaranteeForestNear(GameMap map, int centreCol, int centreRow) {
        for (Hex hex : map.withinRange(map.get(centreCol, centreRow), 2)) {
            if (hex.getTerrain() == Terrain.FOREST) {
                return;
            }
        }
        int[] spot = HexGeometry.neighbour(centreCol, centreRow, 0);
        if (map.inside(spot[0], spot[1])) {
            map.set(new Hex(spot[0], spot[1], Terrain.FOREST, ResourceType.WOOD, 80));
        }
    }
}
