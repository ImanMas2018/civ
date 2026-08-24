package civ.model;

import civ.model.world.Happiness;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything the player owns: stockpile, units, buildings, researched techs. */
public class Empire {

    // Temporary test stash so upgrades and techs can be tried without grinding.
    private final Stockpile stock = new Stockpile(9999);
    private final List<Unit> units = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final Set<Tech> techs = EnumSet.noneOf(Tech.class);
    private final Happiness happiness = new Happiness();

    private TownHall townHall;
    private int unitCap = 8;

    public Stockpile getStock() {
        return stock;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public TownHall getTownHall() {
        return townHall;
    }

    public int getUnitCap() {
        return unitCap;
    }

    public void setTownHall(TownHall townHall) {
        this.townHall = townHall;
        if (townHall != null && !buildings.contains(townHall)) {
            buildings.add(townHall);
        }
    }

    public void raiseUnitCap(int by) {
        unitCap += by;
    }

    public void setUnitCap(int unitCap) {
        this.unitCap = unitCap;
    }

    public boolean hasTech(Tech tech) {
        return techs.contains(tech);
    }

    public void addTech(Tech tech) {
        techs.add(tech);
        if (tech == Tech.STORAGE_1 || tech == Tech.STORAGE_2) {
            stock.setCapacity(stock.getCapacity() + 100);
        }
        if (tech == Tech.DEFENSIVE_ARCHITECTURE && townHall != null) {
            townHall.applyDefensiveArchitecture();
        }
    }

    public Set<Tech> getTechs() {
        return techs;
    }

    public boolean canResearch(Tech tech) {
        if (hasTech(tech)) {
            return false;
        }
        if (tech.getRequired() != null && !hasTech(tech.getRequired())) {
            return false;
        }
        if (townHall != null && tech.getRequiredLevel() > townHall.getLevel()) {
            return false;
        }
        return stock.canPay(tech.getWoodCost(), tech.getStoneCost(), tech.getIronCost());
    }

    public boolean isAtUnitCap() {
        return units.size() >= unitCap;
    }

    public int countMilitary() {
        int n = 0;
        for (Unit unit : units) {
            if (unit.isMilitary()) {
                n++;
            }
        }
        return n;
    }

    /** Follows Town Hall level (5 / 10 / 15). Level 1 until upgrades exist. */
    public int getMilitaryCap() {
        return 5 * townHall.getLevel();
    }

    public int countUnits(String typeName) {
        int n = 0;
        for (Unit unit : units) {
            if (unit.getTypeName().equals(typeName)) {
                n++;
            }
        }
        return n;
    }

    /**
     * What the HUD shows as "+3 per turn". Recomputed from scratch every time
     * it is asked, so it can never drift out of sync with reality.
     */
    public Map<ResourceType, Integer> netRatePerTurn(GameMap map,
                                                     java.util.List<civ.model.tribe.Tribe> tribes) {
        Map<ResourceType, Integer> rate = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            rate.put(type, 0);
        }

        rate.put(ResourceType.FOOD, rate.get(ResourceType.FOOD) + 1);
        rate.put(ResourceType.WOOD, rate.get(ResourceType.WOOD) + 1);

        for (Building building : buildings) {
            ResourceType out = building.getType().getProduces();
            if (out != null) {
                rate.put(out, rate.get(out) + building.outputPerTurn(this, map));
            }
            ResourceType upkeep = building.getType().getUpkeepResource();
            if (upkeep != null) {
                rate.put(upkeep, rate.get(upkeep) - building.getType().getUpkeepAmount());
            }
        }

        rate.put(ResourceType.FOOD,
                rate.get(ResourceType.FOOD) + Adjacency.farmPairs(this, map)
                        + Adjacency.allyFarmBonus(this, tribes) - units.size());
        rate.put(ResourceType.STONE,
                rate.get(ResourceType.STONE) + Adjacency.allyMineBonus(this, tribes));
        return rate;
    }

    public Happiness getHappiness() {
        return happiness;
    }

    public int happiness() {
        return happiness.getValue();
    }

    public void addHappinessBonus(int delta) {
        happiness.add(delta);
    }
}
