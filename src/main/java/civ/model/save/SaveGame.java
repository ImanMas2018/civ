package civ.model.save;

import java.util.ArrayList;
import java.util.List;

public class SaveGame {

    public int saveVersion = 1;
    public String saveName;
    public long savedAtMillis;
    public String summary;

    public long mapSeed;
    public int turn;
    public int happiness;
    public boolean militaryCapHit;
    public String randomState;
    public boolean starving;
    public boolean nextDockHalfPrice;
    public int unitCap;
    public int lastBearTurn;
    public int stockCapacity;
    public int food;
    public int wood;
    public int stone;
    public int iron;

    /** Per-player fog grids (single-player: one entry). Prefer this over HexData flags. */
    public boolean[][] fogDiscovered;
    public boolean[][] fogOwned;
    public String playerName;
    public String playerColour;

    public int townHallLevel;
    public int townHallHp;
    public int townHallMaxHp;
    public int townHallDefence;
    public boolean townHallWall;
    public String queueKind;
    public String queueTarget;
    public int queueTurnsLeft;

    public List<String> techs = new ArrayList<>();
    public List<HexData> hexes = new ArrayList<>();
    public List<EdgeData> edges = new ArrayList<>();
    public List<BuildingData> buildings = new ArrayList<>();
    public List<UnitData> units = new ArrayList<>();
    public List<UnitData> hostiles = new ArrayList<>();
    public List<TribeData> tribes = new ArrayList<>();
    public List<String> log = new ArrayList<>();

    public static class HexData {
        public int col;
        public int row;
        public int depositAmount;
        public boolean discovered;
        public boolean owned;
        public boolean reserved;
        public boolean road;
        public boolean blocked;
    }

    public static class EdgeData {
        public int col1;
        public int row1;
        public int col2;
        public int row2;
        public boolean river;
        public Integer wallHp;
    }

    public static class BuildingData {
        public int id;
        public String type;
        public int col;
        public int row;
        public int hp;
        public int maxHp;
        public int unpaidTurns;
        public int pausedUntilTurn;
    }

    public static class UnitData {
        public int id;
        public String type;
        public int col;
        public int row;
        public int ap;
        public int bodyHp;
        public int maxBodyHp;
        public Integer combatHp;
        public Integer maxCombatHp;
        public Integer attackPower;
        public Integer attackRange;
        public Integer stationBuildingId;
        public Long tribeId;
    }

    public static class TribeData {
        public long id;
        public String name;
        public String type;
        public int campCol;
        public int campRow;
        public int relation;
        public boolean discovered;
        public boolean destroyed;
        public boolean allied;
        public int tradeBonusPercent;
        public int questBlockTurns;
        public int turnsSinceGuardSpawn;
        public int turnsSinceQuestOffer;
        public int campHp;
        public int campMaxHp;
        public String questStatus;
        public int questTurnsLeft;
        public int questProgress;
        public List<UnitData> guards = new ArrayList<>();
    }
}
