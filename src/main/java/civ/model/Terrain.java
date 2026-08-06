package civ.model;

/** Ground type of a hex. Movement cost lives here (property of the terrain). */
public enum Terrain {
    PLAINS("Plains", 1),
    GRASSLAND("Grassland", 1),
    FOREST("Forest", 2),
    MOUNTAIN("Mountain", 4);

    private final String label;
    private final int moveCost;

    Terrain(String label, int moveCost) {
        this.label = label;
        this.moveCost = moveCost;
    }

    public String getLabel() {
        return label;
    }

    public int getMoveCost() {
        return moveCost;
    }
}
