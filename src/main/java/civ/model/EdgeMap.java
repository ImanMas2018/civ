package civ.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EdgeMap {

    private final Map<Edge, Edge> edges = new HashMap<>();

    public Edge getOrCreate(Hex a, Hex b) {
        Edge key = new Edge(a, b);
        Edge existing = edges.get(key);
        if (existing != null) {
            return existing;
        }
        edges.put(key, key);
        return key;
    }

    public Edge find(Hex a, Hex b) {
        if (a == null || b == null) {
            return null;
        }
        return edges.get(new Edge(a, b));
    }

    public Collection<Edge> all() {
        return edges.values();
    }
}
