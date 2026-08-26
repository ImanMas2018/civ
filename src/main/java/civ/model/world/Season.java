package civ.model.world;

public enum Season {
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter");

    private final String label;

    Season(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Turn 1–10 spring, 11–20 summer, 21–30 autumn, 31–40 winter, then repeat. */
    public static Season forTurn(int turn) {
        int index = ((Math.max(1, turn) - 1) / 10) % 4;
        return values()[index];
    }

    /** Extra food per farm and stable this season. */
    public int farmFoodBonus() {
        if (this == SPRING) {
            return 1;
        }
        if (this == WINTER) {
            return -1;
        }
        return 0;
    }

    /** Extra AP on every land step for all units. */
    public int landMovePenalty() {
        return this == WINTER ? 1 : 0;
    }

    /** Extra AP when stepping onto a water hex. */
    public int waterMovePenalty() {
        return this == AUTUMN ? 1 : 0;
    }

    public boolean hasSnow() {
        return this == WINTER;
    }

    public boolean hasRain() {
        return this == AUTUMN;
    }
}
