package civ.model.map;

import java.io.IOException;
import java.util.List;

public final class MapCatalog {

    private MapCatalog() {
    }

    public static List<MapPreset> all() throws IOException {
        return List.of(
                new MapPreset("crossroads.map"),
                new MapPreset("two-rivers.map"),
                new MapPreset("highlands.map"));
    }

    public static MapPreset crossroads() throws IOException {
        return new MapPreset("crossroads.map");
    }
}
