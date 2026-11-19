package voidclient.modules.basehunting;

import java.util.EnumMap;
import java.util.Map;

/** Component counts for one chunk, as visible to the client at scan time. */
public final class ChunkStats {
    public final int cx, cz;
    public final String dimension;
    public final Map<Component, Integer> counts = new EnumMap<>(Component.class);
    public long scannedAt = System.currentTimeMillis();
    public int topY = Integer.MIN_VALUE;

    public ChunkStats(int cx, int cz, String dimension) {
        this.cx = cx;
        this.cz = cz;
        this.dimension = dimension;
    }

    public int get(Component c) { return counts.getOrDefault(c, 0); }

    public int total() {
        int t = 0;
        for (int v : counts.values()) t += v;
        return t;
    }

    public int group(Component.Group g) {
        int t = 0;
        for (var e : counts.entrySet()) if (e.getKey().group == g) t += e.getValue();
        return t;
    }

    public Component.Group dominant() {
        Component.Group best = null;
        int max = 0;
        for (Component.Group g : Component.Group.values()) {
            int v = group(g);
            if (v > max) {
                max = v;
                best = g;
            }
        }
        return best;
    }
}
