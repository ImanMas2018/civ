package civ.model.map;

import civ.model.GameMap;
import civ.model.Hex;
import civ.model.ResourceType;
import civ.model.Terrain;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapPreset {

    private final String resourceName;
    private String displayName = "Unnamed";
    private final List<int[]> spawns = new ArrayList<>();
    private final List<String> grid = new ArrayList<>();

    public MapPreset(String resourceName) throws IOException {
        this.resourceName = resourceName;
        load();
    }

    private void load() throws IOException {
        InputStream in = getClass().getResourceAsStream("/maps/" + resourceName);
        if (in == null) {
            throw new IOException("Map not found: " + resourceName);
        }

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("name=")) {
                    displayName = line.substring(5).trim();
                } else if (line.startsWith("spawns=")) {
                    for (String pair : line.substring(7).trim().split(" ")) {
                        if (pair.isEmpty()) {
                            continue;
                        }
                        String[] parts = pair.split(",");
                        spawns.add(new int[]{Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1])});
                    }
                } else {
                    grid.add(line);
                }
            }
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxPlayers() {
        return spawns.size();
    }

    public int[] spawnFor(int index) {
        return spawns.get(index);
    }

    public GameMap build() {
        GameMap map = new GameMap(grid.get(0).length(), grid.size());
        for (int row = 0; row < grid.size(); row++) {
            String line = grid.get(row);
            for (int col = 0; col < line.length(); col++) {
                char symbol = line.charAt(col);
                map.set(new Hex(col, row, terrainFor(symbol),
                        depositFor(symbol), amountFor(symbol)));
            }
        }
        return map;
    }

    private Terrain terrainFor(char symbol) {
        switch (symbol) {
            case 'G':
            case 'A':
                return Terrain.GRASSLAND;
            case 'F':
                return Terrain.FOREST;
            case 'M':
            case 'I':
                return Terrain.MOUNTAIN;
            case 'R':
                return Terrain.MOUNTAIN_RANGE;
            case 'S':
                return Terrain.SEA;
            default:
                return Terrain.PLAINS;
        }
    }

    /** Deposits: forests→wood, mountains→stone, I→iron, A→food, sea→fish chance. */
    private ResourceType depositFor(char symbol) {
        switch (symbol) {
            case 'F':
                return ResourceType.WOOD;
            case 'M':
                return ResourceType.STONE;
            case 'I':
                return ResourceType.IRON;
            case 'A':
                return ResourceType.FOOD;
            case 'S':
                return ResourceType.FOOD;
            default:
                return null;
        }
    }

    private int amountFor(char symbol) {
        switch (symbol) {
            case 'F':
                return 80;
            case 'M':
                return 60;
            case 'I':
                return 50;
            case 'A':
                return 90;
            case 'S':
                return 50;
            default:
                return 0;
        }
    }
}
