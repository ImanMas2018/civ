package civ.net.protocol.dto;

import java.util.ArrayList;
import java.util.List;

/** Everything one specific player is allowed to know, right now. */
public class GameStateDto {

    public int turn;
    public long currentPlayerId;
    public long yourPlayerId;
    public String season;
    public String mapName;

    public List<PlayerDto> players = new ArrayList<>();
    public List<HexDto> hexes = new ArrayList<>();
    public List<UnitDto> units = new ArrayList<>();
    public List<BuildingDto> buildings = new ArrayList<>();
    public List<EdgeDto> edges = new ArrayList<>();
    public List<TribeDto> tribes = new ArrayList<>();
    public StockDto yourStock;
    public List<String> yourTechs = new ArrayList<>();
    public int yourUnitCap;
    public int yourHappiness;
    public boolean yourStarving;
    public List<String> log = new ArrayList<>();

    public static class HexDto {
        public int col;
        public int row;
        public String terrain;
        public String deposit;
        public int depositAmount;
        public boolean road;
        public boolean blocked;
        public boolean reserved;
        public Long ownerId;
        public boolean currentlyVisible;
    }

    public static class UnitDto {
        public long id;
        public long createdAt;
        public long ownerId;
        public String type;
        public int col;
        public int row;
        public int ap;
        public int maxAp;
        public int hp;
        public int maxHp;
        public int combatHp;
        public int maxCombatHp;
        public int charges = -1;
        public boolean stationed;
        public boolean hostile;
    }

    public static class BuildingDto {
        public long id;
        public long createdAt;
        public long ownerId;
        public String type;
        public int col;
        public int row;
        public int hp;
        public int maxHp;
        public int level;
        public boolean defensiveWall;
        public String queue;
        public List<Long> workerIds = new ArrayList<>();
    }

    public static class EdgeDto {
        public int col1;
        public int row1;
        public int col2;
        public int row2;
        public boolean river;
        public boolean wall;
        public int wallHp;
    }

    public static class PlayerDto {
        public long id;
        public long createdAt;
        public String name;
        public String colour;
        public boolean alive;
        public boolean connected;
        public String diplomacyWithYou = "NEUTRAL";
    }

    public static class StockDto {
        public int food;
        public int wood;
        public int stone;
        public int iron;
        public int capacity;
    }

    public static class TribeDto {
        public long id;
        public String name;
        public String type;
        public int campCol;
        public int campRow;
        public boolean discovered;
        public boolean destroyed;
        public int relation;
        public int campHp;
        public int campMaxHp;
    }
}
