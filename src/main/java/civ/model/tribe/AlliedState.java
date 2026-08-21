package civ.model.tribe;

public class AlliedState implements RelationState {
    @Override public String getName() { return "Allied"; }
    @Override public boolean allowsGift() { return true; }
    @Override public boolean allowsTrade() { return true; }
    @Override public boolean allowsQuest() { return true; }
    @Override public boolean allowsAlliance() { return false; }
    @Override public boolean allowsWarDeclaration() { return true; }
    @Override public boolean allowsPeaceRequest() { return false; }
    @Override public boolean isHostile() { return false; }
    @Override public String lockReason() { return "Already allied."; }
}
