package civ.model;

import java.awt.Color;

public enum PlayerColour {
    BLUE(new Color(70, 130, 220)),
    RED(new Color(200, 70, 70)),
    GREEN(new Color(60, 160, 90)),
    GOLD(new Color(210, 170, 50));

    private final Color awt;

    PlayerColour(Color awt) {
        this.awt = awt;
    }

    public Color getAwt() {
        return awt;
    }

    public static PlayerColour forIndex(int index) {
        PlayerColour[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
