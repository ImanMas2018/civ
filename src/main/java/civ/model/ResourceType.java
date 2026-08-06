package civ.model;

/** The four empire resources. A hex deposit is also a ResourceType (or null if empty). */
public enum ResourceType {
    FOOD("Food"),
    WOOD("Wood"),
    STONE("Stone"),
    IRON("Iron");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
