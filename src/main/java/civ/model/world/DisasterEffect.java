package civ.model.world;

import civ.model.Hex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Snapshot the view animates after a disaster strikes. */
public class DisasterEffect {

    private final String name;
    private final Hex epicenter;
    private final List<Hex> affected;
    private final boolean visible;

    public DisasterEffect(String name, Hex epicenter, List<Hex> affected, boolean visible) {
        this.name = name;
        this.epicenter = epicenter;
        this.affected = Collections.unmodifiableList(new ArrayList<>(affected));
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public Hex getEpicenter() {
        return epicenter;
    }

    public List<Hex> getAffected() {
        return affected;
    }

    public boolean isVisible() {
        return visible;
    }
}
