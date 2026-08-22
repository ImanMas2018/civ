package civ.model.factory;

import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Hex;
import civ.model.Outpost;
import civ.model.ProductionBuilding;
import civ.model.TownHall;
import civ.model.TradingPost;
import civ.model.event.EventBus;
import civ.model.event.GameEvent;

/** One place that knows how to construct every building type. */
public class BuildingFactory {

    private final EventBus bus;

    public BuildingFactory(EventBus bus) {
        this.bus = bus;
    }

    public Building create(BuildingType type, Hex hex) {
        Building building;
        if (type == BuildingType.TOWN_HALL) {
            building = new TownHall(hex);
        } else if (type == BuildingType.TRADING_POST) {
            building = new TradingPost(hex);
        } else if (type == BuildingType.OUTPOST) {
            building = new Outpost(hex);
        } else {
            building = new ProductionBuilding(type, hex);
        }
        hex.setBuilding(building);
        bus.publish(GameEvent.BUILDING_PLACED, building);
        return building;
    }
}
