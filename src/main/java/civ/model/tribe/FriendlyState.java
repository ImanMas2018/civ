package civ.model.tribe;

public class FriendlyState implements RelationState {
    @Override public String getName() { return "Friendly"; }
    @Override public boolean allowsGift() { return true; }
    @Override public boolean allowsTrade() { return true; }
    @Override public boolean allowsQuest() { return true; }
    @Override public boolean allowsAlliance() { return true; }
    @Override public boolean allowsWarDeclaration() { return true; }
    @Override public boolean allowsPeaceRequest() { return false; }
    @Override public boolean isHostile() { return false; }
    @Override public String lockReason() {
        return "Alliance needs a relation of at least 70.";
    }
}
