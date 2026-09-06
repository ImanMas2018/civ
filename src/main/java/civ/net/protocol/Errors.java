package civ.net.protocol;

/** Every rejection message in the game, in one place. */
public final class Errors {

    public static final String NOT_YOUR_TURN = "It is not your turn.";
    public static final String NOT_YOUR_UNIT = "That unit does not belong to you.";
    public static final String NOT_YOUR_BUILDING = "That building does not belong to you.";
    public static final String NOT_ENOUGH_AP = "This unit does not have enough action points.";
    public static final String NOT_AT_WAR = "You are not at war with this player.";
    public static final String ALLIED = "You cannot attack an ally.";
    public static final String HOST_ONLY = "Only the host can do that.";
    public static final String HOST_ONLY_START = "Only the host can start the game.";
    public static final String NOT_ALL_READY = "All players must be ready before the game starts.";
    public static final String ITEM_USED = "This unit has already used an item this turn.";
    public static final String FOGGED_TARGET = "You cannot teleport into unexplored territory.";
    public static final String OCCUPIED_TARGET = "That hex already contains a unit.";
    public static final String NAME_TAKEN = "That username is already taken.";

    public static String notEnough(String resource) {
        return "You do not have enough " + resource + " for this.";
    }

    private Errors() {
    }
}
