package civ.model.save;

/** One row in the pause-menu save list. */
public class SaveSlotInfo {

    public enum Kind {
        MANUAL,
        AUTOSAVE
    }

    public enum Status {
        EMPTY,
        OK,
        DAMAGED
    }

    private final String slotName;
    private final Kind kind;
    private Status status;
    private String displayName;
    private int turn;
    private String season;
    private long savedAtMillis;
    private int townHallLevel;
    private String summary;

    public SaveSlotInfo(String slotName, Kind kind) {
        this.slotName = slotName;
        this.kind = kind;
        this.status = Status.EMPTY;
    }

    public String getSlotName() {
        return slotName;
    }

    public Kind getKind() {
        return kind;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public long getSavedAtMillis() {
        return savedAtMillis;
    }

    public void setSavedAtMillis(long savedAtMillis) {
        this.savedAtMillis = savedAtMillis;
    }

    public int getTownHallLevel() {
        return townHallLevel;
    }

    public void setTownHallLevel(int townHallLevel) {
        this.townHallLevel = townHallLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String describe() {
        if (status == Status.EMPTY) {
            return slotLabel() + " — empty";
        }
        if (status == Status.DAMAGED) {
            return slotLabel() + " — damaged";
        }
        return slotLabel() + " — " + displayName
                + "  turn " + turn
                + "  " + season
                + "  TH L" + townHallLevel
                + "  " + formatTime(savedAtMillis)
                + (summary == null ? "" : "  " + summary);
    }

    private String slotLabel() {
        return kind == Kind.AUTOSAVE ? "Autosave" : slotName;
    }

    private static String formatTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(millis));
    }
}
