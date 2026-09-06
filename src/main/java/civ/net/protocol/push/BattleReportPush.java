package civ.net.protocol.push;

import civ.net.protocol.Broadcast;

/** Placeholder for Step 4. */
public class BattleReportPush extends Broadcast {

    public static final String TYPE = "battle_report";

    private final String summary;

    public BattleReportPush(String summary) {
        super(TYPE);
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }
}
