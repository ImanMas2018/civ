package civ.model.tribe;

import civ.model.ResourceType;
import civ.model.Terrain;

public enum TribeType {
    FARMER("Farmer", 40, Terrain.GRASSLAND,
            "Friendly: easier food trades. Allied: +1 food from each Farm.",
            ResourceType.FOOD),
    WARRIOR("Warrior", 70, Terrain.PLAINS,
            "Friendly: military help quests. Allied: three free swordsmen once.",
            null),
    TRADER("Trader", 50, Terrain.PLAINS,
            "Friendly: trade any resource at 80%. Allied: +10% trade rate permanently.",
            null),
    MOUNTAIN("Mountain", 50, Terrain.MOUNTAIN,
            "Friendly: stone/iron trades at 75%. Allied: +1 stone from each Stone Mine.",
            ResourceType.STONE),
    COASTAL("Coastal", 50, Terrain.GRASSLAND,
            "Friendly: food trades at 75%. Allied: next Dock costs half wood.",
            ResourceType.FOOD);

    private final String label;
    private final int campHp;
    private final Terrain preferredTerrain;
    private final String rewardBlurb;
    private final ResourceType defaultReceive;

    TribeType(String label, int campHp, Terrain preferredTerrain,
              String rewardBlurb, ResourceType defaultReceive) {
        this.label = label;
        this.campHp = campHp;
        this.preferredTerrain = preferredTerrain;
        this.rewardBlurb = rewardBlurb;
        this.defaultReceive = defaultReceive;
    }

    public String getLabel() {
        return label;
    }

    public int getCampHp() {
        return campHp;
    }

    public Terrain getPreferredTerrain() {
        return preferredTerrain;
    }

    public String getRewardBlurb() {
        return rewardBlurb;
    }

    public ResourceType getDefaultReceive() {
        return defaultReceive;
    }
}
