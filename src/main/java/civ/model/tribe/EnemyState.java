package civ.model.tribe;

public class EnemyState implements RelationState {
    @Override public String getName() { return "Enemy"; }
    @Override public boolean allowsGift() { return false; }
    @Override public boolean allowsTrade() { return false; }
    @Override public boolean allowsQuest() { return false; }
    @Override public boolean allowsAlliance() { return false; }
    @Override public boolean allowsWarDeclaration() { return false; }
    @Override public boolean allowsPeaceRequest() { return true; }
    @Override public boolean isHostile() { return true; }
    @Override public String lockReason() { return "At war with this tribe."; }
}
