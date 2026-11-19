package voidclient.modules.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import voidclient.VoidClient;
import voidclient.config.JsonFiles;
import voidclient.events.Events;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Waypoints per world/server, stored in config/voidclient/waypoints/<world>.json. */
public final class WaypointStore {
    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static String worldKey;
    private static boolean initialised;

    private WaypointStore() {}

    public static void init() {
        if (initialised) return;
        initialised = true;
        var bus = VoidClient.get().events();
        bus.subscribe(Events.WorldJoin.class, e -> load(e.worldKey()));
        bus.subscribe(Events.WorldLeave.class, e -> {
            save();
            WAYPOINTS.clear();
            worldKey = null;
        });
    }

    public static String worldKey() { return worldKey; }

    private static Path file() { return VoidClient.get().config().dir().resolve("waypoints").resolve(worldKey + ".json"); }

    private static void load(String key) {
        worldKey = key;
        WAYPOINTS.clear();
        JsonObject o = JsonFiles.read(file());
        if (o != null && o.get("waypoints") instanceof JsonArray a) {
            for (JsonElement e : a) {
                try {
                    WAYPOINTS.add(Waypoint.fromJson(e.getAsJsonObject()));
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public static void save() {
        if (worldKey == null) return;
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (Waypoint w : WAYPOINTS) a.add(w.toJson());
        o.add("waypoints", a);
        JsonFiles.write(file(), o);
    }

    public static List<Waypoint> all() { return WAYPOINTS; }

    public static String currentDimension() {
        var w = MinecraftClient.getInstance().world;
        return w == null ? "" : w.getRegistryKey().getValue().toString();
    }

    public static List<Waypoint> inCurrentDimension() {
        String dim = currentDimension();
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : WAYPOINTS) if (w.dimension.equals(dim)) out.add(w);
        return out;
    }

    public static Waypoint add(String name, int x, int y, int z, int color, String type) {
        if (worldKey == null) return null;
        Waypoint w = new Waypoint(name, x, y, z, currentDimension(), color);
        w.type = type;
        WAYPOINTS.add(w);
        save();
        return w;
    }

    public static void remove(Waypoint w) {
        WAYPOINTS.remove(w);
        save();
    }
}
