package civ.model.tribe;

public interface RelationState {

    String getName();

    boolean allowsGift();

    boolean allowsTrade();

    boolean allowsQuest();

    boolean allowsAlliance();

    boolean allowsWarDeclaration();

    boolean allowsPeaceRequest();

    boolean isHostile();

    /** Why a locked action is locked — goes into the button tooltip. */
    String lockReason();
}
