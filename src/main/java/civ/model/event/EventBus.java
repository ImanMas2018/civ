package civ.model.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {

    private final Map<GameEvent, List<Consumer<Object>>> listeners =
            new EnumMap<>(GameEvent.class);

    /** "Whenever this event happens, run this code." */
    public void subscribe(GameEvent event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, key -> new ArrayList<>()).add(listener);
    }

    /** "This just happened." The publisher does not know who is listening. */
    public void publish(GameEvent event, Object payload) {
        List<Consumer<Object>> registered = listeners.get(event);
        if (registered == null) {
            return;
        }
        for (Consumer<Object> listener : new ArrayList<>(registered)) {
            listener.accept(payload);
        }
    }
}
