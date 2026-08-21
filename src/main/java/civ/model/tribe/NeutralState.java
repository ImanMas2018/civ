package civ.model.tribe;

public class NeutralState implements RelationState {
    @Override public String getName() { return "Neutral"; }
    @Override public boolean allowsGift() { return true; }
    @Override public boolean allowsTrade() { return false; }
    @Override public boolean allowsQuest() { return false; }
    @Override public boolean allowsAlliance() { return false; }
    @Override public boolean allowsWarDeclaration() { return true; }
    @Override public boolean allowsPeaceRequest() { return false; }
    @Override public boolean isHostile() { return false; }
    @Override public String lockReason() {
        return "Relation must be at least 20 for trade and quests.";
    }
}
