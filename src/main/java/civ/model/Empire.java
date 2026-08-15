package civ.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything the player owns: stockpile, units, buildings, researched techs. */
public class Empire {

    private final Stockpile stock = new Stockpile(100);
    private final List<Unit> units = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final Set<Tech> techs = EnumSet.noneOf(Tech.class);

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
        buildings.add(townHall);
    }

    public void raiseUnitCap(int by) {
        unitCap += by;
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
    public Map<ResourceType, Integer> netRatePerTurn() {
        Map<ResourceType, Integer> rate = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            rate.put(type, 0);
        }

        rate.put(ResourceType.FOOD, rate.get(ResourceType.FOOD) + 1);
        rate.put(ResourceType.WOOD, rate.get(ResourceType.WOOD) + 1);

        for (Building building : buildings) {
            ResourceType out = building.getType().getProduces();
            if (out != null) {
                rate.put(out, rate.get(out) + building.outputPerTurn(this));
            }
            ResourceType upkeep = building.getType().getUpkeepResource();
            if (upkeep != null) {
                rate.put(upkeep, rate.get(upkeep) - building.getType().getUpkeepAmount());
            }
        }

        rate.put(ResourceType.FOOD, rate.get(ResourceType.FOOD) - units.size());
        return rate;
    }
}
