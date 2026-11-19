package voidclient.modules.basehunting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.VoidClient;
import voidclient.config.JsonFiles;
import voidclient.events.Events;
import voidclient.utilities.ColorUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Inspection history per world: coordinates, date, counts, labels, notes and bookmarks. */
public final class InspectionStore {
    public static final class Entry {
        public final int cx, cz;
        public final String dimension;
        public long firstSeen = System.currentTimeMillis(), lastInspected = System.currentTimeMillis();
        public final Map<Component, Integer> counts = new EnumMap<>(Component.class);
        public String label = "";
        public String notes = "";
        public boolean bookmarked;

        public Entry(int cx, int cz, String dimension) {
            this.cx = cx;
            this.cz = cz;
            this.dimension = dimension;
        }

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

        public int heatColor() {
            float f = Math.min(1f, total() / 200f);
            return ColorUtil.lerp(0xFF3CB4FF, 0xFFFF2BD6, f);
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("cx", cx);
            o.addProperty("cz", cz);
            o.addProperty("dimension", dimension);
            o.addProperty("firstSeen", firstSeen);
            o.addProperty("lastInspected", lastInspected);
            o.addProperty("label", label);
            o.addProperty("notes", notes);
            o.addProperty("bookmarked", bookmarked);
            JsonObject c = new JsonObject();
            for (var e : counts.entrySet()) c.addProperty(e.getKey().name(), e.getValue());
            o.add("counts", c);
            return o;
        }

        static Entry fromJson(JsonObject o) {
            Entry e = new Entry(o.get("cx").getAsInt(), o.get("cz").getAsInt(), o.get("dimension").getAsString());
            e.firstSeen = o.get("firstSeen").getAsLong();
            e.lastInspected = o.get("lastInspected").getAsLong();
            e.label = o.has("label") ? o.get("label").getAsString() : "";
            e.notes = o.has("notes") ? o.get("notes").getAsString() : "";
            e.bookmarked = o.has("bookmarked") && o.get("bookmarked").getAsBoolean();
            if (o.get("counts") instanceof JsonObject c) {
                for (var k : c.entrySet()) {
                    try {
                        e.counts.put(Component.valueOf(k.getKey()), k.getValue().getAsInt());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return e;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static String worldKey;
    private static boolean dirty, initialised;
    private static int ticks;
    private static int maxEntries = 5000;

    private InspectionStore() {}

    public static void init() {
        if (initialised) return;
        initialised = true;
        var bus = VoidClient.get().events();
        bus.subscribe(Events.WorldJoin.class, e -> load(e.worldKey()));
        bus.subscribe(Events.WorldLeave.class, e -> {
            save();
            ENTRIES.clear();
            worldKey = null;
        });
        bus.subscribe(Events.TickPost.class, e -> {
            if (++ticks % 600 == 0 && dirty) save();
        });
    }

    public static void setMaxEntries(int max) { maxEntries = max; }

    private static Path file() { return VoidClient.get().config().dir().resolve("inspections").resolve(worldKey + ".json"); }

    private static void load(String key) {
        worldKey = key;
        ENTRIES.clear();
        JsonObject o = JsonFiles.read(file());
        if (o != null && o.get("entries") instanceof JsonArray a) {
            for (JsonElement e : a) {
                try {
                    ENTRIES.add(Entry.fromJson(e.getAsJsonObject()));
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public static void save() {
        if (worldKey == null) return;
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (Entry e : ENTRIES) a.add(e.toJson());
        o.add("entries", a);
        JsonFiles.write(file(), o);
        dirty = false;
    }

    public static void markDirty() { dirty = true; }

    public static List<Entry> entries() { return ENTRIES; }

    public static Entry find(int cx, int cz, String dim) {
        for (Entry e : ENTRIES) if (e.cx == cx && e.cz == cz && e.dimension.equals(dim)) return e;
        return null;
    }

    public static void record(ChunkStats s) {
        if (worldKey == null) return;
        Entry e = find(s.cx, s.cz, s.dimension);
        if (e == null) {
            e = new Entry(s.cx, s.cz, s.dimension);
            ENTRIES.add(e);
            while (ENTRIES.size() > maxEntries) {
                Entry victim = null;
                for (Entry x : ENTRIES) if (!x.bookmarked && (victim == null || x.lastInspected < victim.lastInspected)) victim = x;
                if (victim == null) break;
                ENTRIES.remove(victim);
            }
        }
        e.counts.clear();
        e.counts.putAll(s.counts);
        e.lastInspected = s.scannedAt;
        dirty = true;
    }

    public static void clear() {
        ENTRIES.removeIf(e -> !e.bookmarked);
        dirty = true;
    }

    public static void prune(long olderThanMs) {
        long cutoff = System.currentTimeMillis() - olderThanMs;
        ENTRIES.removeIf(e -> !e.bookmarked && e.lastInspected < cutoff);
        dirty = true;
    }
}
