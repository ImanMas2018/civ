package civ.model.item;

public final class ItemFactory {

    private ItemFactory() {
    }

    public static Item create(ItemType type) {
        switch (type) {
            case TELEPORT:
                return new TeleportItem();
            case MOBILITY:
                return new MobilityItem();
            case COMBAT:
                return new CombatItem();
            default:
                throw new IllegalArgumentException("Unknown item: " + type);
        }
    }
}
