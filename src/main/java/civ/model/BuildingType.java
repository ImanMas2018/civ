package civ.model;

/**
 * Master table for every building. Adding a type in Phase 2 is one new row;
 * existing code does not change (Open/Closed).
 */
public enum BuildingType {

    TOWN_HALL("Town Hall", null, null, null, 0, 0, 0, 0, 0, 0, null, 0, null),
    LUMBER_MILL("Lumber Mill", Terrain.FOREST, ResourceType.WOOD, ResourceType.WOOD,
            2, 3, 10, 0, 0, 1, ResourceType.WOOD, 1, null),
    STONE_MINE("Stone Mine", Terrain.MOUNTAIN, ResourceType.STONE, ResourceType.STONE,
            2, 3, 20, 0, 0, 1, ResourceType.WOOD, 1, Tech.STONE_MINING),
    IRON_MINE("Iron Mine", Terrain.MOUNTAIN, ResourceType.IRON, ResourceType.IRON,
            1, 2, 30, 10, 0, 2, ResourceType.WOOD, 1, Tech.IRON_MINING),
    FARM("Farm", Terrain.GRASSLAND, ResourceType.FOOD, ResourceType.FOOD,
            3, 3, 15, 0, 0, 1, ResourceType.WOOD, 1, null),
    STABLE("Stable", Terrain.PLAINS, ResourceType.FOOD, ResourceType.FOOD,
            2, 2, 20, 0, 0, 1, ResourceType.WOOD, 1, null),
    SETTLEMENT("Settlement", null, null, null,
            0, 0, 40, 40, 10, 2, ResourceType.STONE, 1, Tech.TOWN_BUILDING);

    private final String label;
    private final Terrain requiredTerrain;
    private final ResourceType requiredDeposit;
    private final ResourceType produces;
    private final int ratePerWorker;
    private final int workerCapacity;
    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final int apCost;
    private final ResourceType upkeepResource;
    private final int upkeepAmount;
    private final Tech requiredTech;

    BuildingType(String label, Terrain requiredTerrain, ResourceType requiredDeposit,
                 ResourceType produces, int ratePerWorker, int workerCapacity,
                 int woodCost, int stoneCost, int ironCost, int apCost,
                 ResourceType upkeepResource, int upkeepAmount, Tech requiredTech) {
        this.label = label;
        this.requiredTerrain = requiredTerrain;
        this.requiredDeposit = requiredDeposit;
        this.produces = produces;
        this.ratePerWorker = ratePerWorker;
        this.workerCapacity = workerCapacity;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
        this.apCost = apCost;
        this.upkeepResource = upkeepResource;
        this.upkeepAmount = upkeepAmount;
        this.requiredTech = requiredTech;
    }

    public String getLabel() {
        return label;
    }

    public Terrain getRequiredTerrain() {
        return requiredTerrain;
    }

    public ResourceType getRequiredDeposit() {
        return requiredDeposit;
    }

    public ResourceType getProduces() {
        return produces;
    }

    public int getRatePerWorker() {
        return ratePerWorker;
    }

    public int getWorkerCapacity() {
        return workerCapacity;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    public int getIronCost() {
        return ironCost;
    }

    public int getApCost() {
        return apCost;
    }

    public ResourceType getUpkeepResource() {
        return upkeepResource;
    }

    public int getUpkeepAmount() {
        return upkeepAmount;
    }

    public Tech getRequiredTech() {
        return requiredTech;
    }
}
