package voidclient.modules.world;

import com.google.gson.JsonObject;

/** A saved location in one dimension of one world/server. */
public final class Waypoint {
    public String name;
    public int x, y, z;
    public String dimension;
    public int color;
    public boolean visible = true;
    public String type = "Manual";
    public long created = System.currentTimeMillis();

    public Waypoint(String name, int x, int y, int z, String dimension, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        o.addProperty("dimension", dimension);
        o.addProperty("color", String.format("#%08X", color));
        o.addProperty("visible", visible);
        o.addProperty("type", type);
        o.addProperty("created", created);
        return o;
    }

    public static Waypoint fromJson(JsonObject o) {
        Waypoint w = new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(),
                o.get("dimension").getAsString(), voidclient.utilities.ColorUtil.parseHex(o.get("color").getAsString(), 0xFF00F0FF));
        if (o.has("visible")) w.visible = o.get("visible").getAsBoolean();
        if (o.has("type")) w.type = o.get("type").getAsString();
        if (o.has("created")) w.created = o.get("created").getAsLong();
        return w;
    }
}
