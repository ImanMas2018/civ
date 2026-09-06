package civ.model.diplomacy;

import civ.model.Player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Symmetric table keyed by an unordered pair of player ids — same trick as {@code Edge}. */
public class DiplomacyTable {

    private final Map<String, DiplomaticState> states = new HashMap<>();
    /** Pending alliance proposals keyed as "fromId:toId" (directed). */
    private final Set<String> pendingAlliances = new HashSet<>();

    private String key(long a, long b) {
        return Math.min(a, b) + ":" + Math.max(a, b);
    }

    private String proposalKey(long from, long to) {
        return from + ":" + to;
    }

    public DiplomaticState between(Player a, Player b) {
        if (a == null || b == null) {
            return DiplomaticState.NEUTRAL;
        }
        if (a.getId() == b.getId()) {
            return DiplomaticState.ALLIED;
        }
        return states.getOrDefault(key(a.getId(), b.getId()), DiplomaticState.NEUTRAL);
    }

    public void set(Player a, Player b, DiplomaticState state) {
        if (a == null || b == null || a.getId() == b.getId()) {
            return;
        }
        states.put(key(a.getId(), b.getId()), state);
        clearProposalsBetween(a.getId(), b.getId());
    }

    public boolean canAttack(Player attacker, Player defender) {
        return between(attacker, defender).allowsAttack();
    }

    public boolean sharesVision(Player a, Player b) {
        return between(a, b).sharesVision();
    }

    public void proposeAlliance(long fromId, long toId) {
        pendingAlliances.add(proposalKey(fromId, toId));
    }

    public boolean hasProposal(long fromId, long toId) {
        return pendingAlliances.contains(proposalKey(fromId, toId));
    }

    public void clearProposal(long fromId, long toId) {
        pendingAlliances.remove(proposalKey(fromId, toId));
    }

    private void clearProposalsBetween(long a, long b) {
        pendingAlliances.remove(proposalKey(a, b));
        pendingAlliances.remove(proposalKey(b, a));
    }

    public void clear() {
        states.clear();
        pendingAlliances.clear();
    }
}
