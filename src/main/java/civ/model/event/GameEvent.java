package civ.model.event;

/** Facts the model publishes. Listeners decide what to do; the publisher does not. */
public enum GameEvent {
    TURN_STARTED,
    TURN_ENDED,
    BUILDING_PLACED,
    BUILDING_DESTROYED,
    UNIT_KILLED,
    RELATION_CHANGED,
    SEASON_CHANGED,
    DISASTER_HAPPENED
}
