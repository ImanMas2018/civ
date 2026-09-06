package civ.model;

public class Player extends Entity {

    private final String name;
    private final PlayerColour colour;
    private final Empire empire = new Empire();
    private final Fog fog;

    private boolean alive = true;
    private boolean connected = true;
    private boolean host;
    private boolean starving;

    public Player(String name, PlayerColour colour, GameMap map) {
        this.name = name;
        this.colour = colour;
        this.fog = new Fog(map.getCols(), map.getRows());
    }

    public Player(long id, long createdAt, String name, PlayerColour colour, GameMap map) {
        super(id, createdAt);
        this.name = name;
        this.colour = colour;
        this.fog = new Fog(map.getCols(), map.getRows());
    }

    public String getName() {
        return name;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public Empire getEmpire() {
        return empire;
    }

    public Fog getFog() {
        return fog;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    /** A player is out when their last Town Hall falls. */
    public void eliminate() {
        this.alive = false;
    }

    public boolean hasTownHall() {
        for (Building building : empire.getBuildings()) {
            if (building.getType() == BuildingType.TOWN_HALL && !building.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isStarving() {
        return starving;
    }

    public void setStarving(boolean starving) {
        this.starving = starving;
    }
}
