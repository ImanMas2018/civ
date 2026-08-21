package civ.model.tribe;

import java.util.List;

/** Cross-tribe alliance rules that do not belong in a single RelationState. */
public final class AllianceRules {

    private AllianceRules() {
    }

    public static boolean canAlly(Tribe tribe, List<Tribe> all) {
        if (tribe.getRelation() < 70) {
            return false;
        }
        if (tribe.getState().isHostile()) {
            return false;
        }
        if (tribe.isQuestBlocked()) {
            return false;
        }
        if (tribe.isAllied()) {
            return false;
        }
        for (Tribe other : all) {
            if (!other.isAllied() || other == tribe) {
                continue;
            }
            if (other.getType() == TribeType.WARRIOR) {
                return false;
            }
            if (tribe.getType() == TribeType.WARRIOR) {
                return false;
            }
            if (tribe.getType() == TribeType.FARMER && other.getType() == TribeType.MOUNTAIN) {
                return false;
            }
            if (tribe.getType() == TribeType.MOUNTAIN && other.getType() == TribeType.FARMER) {
                return false;
            }
        }
        return true;
    }

    public static String lockReason(Tribe tribe, List<Tribe> all) {
        if (tribe.isAllied()) {
            return "Already allied.";
        }
        if (tribe.getRelation() < 70) {
            return "Alliance needs a relation of at least 70.";
        }
        if (tribe.getState().isHostile()) {
            return "Cannot ally while at war.";
        }
        if (tribe.isQuestBlocked()) {
            return "A failed quest blocks alliance for 5 turns.";
        }
        for (Tribe other : all) {
            if (!other.isAllied() || other == tribe) {
                continue;
            }
            if (other.getType() == TribeType.WARRIOR || tribe.getType() == TribeType.WARRIOR) {
                return "Allied with the Warrior tribe — cannot ally with others.";
            }
            if ((tribe.getType() == TribeType.FARMER && other.getType() == TribeType.MOUNTAIN)
                    || (tribe.getType() == TribeType.MOUNTAIN && other.getType() == TribeType.FARMER)) {
                return "Cannot ally with Farmer and Mountain at the same time.";
            }
        }
        return "Alliance is not available.";
    }
}
