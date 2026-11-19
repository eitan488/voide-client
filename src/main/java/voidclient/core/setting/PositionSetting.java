package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Resolution-independent screen position: x/y are fractions (0..1) of the scaled screen,
 * applied to the element's anchor corner so layouts survive window and GUI-scale changes.
 */
public class PositionSetting extends Setting<PositionSetting.Pos> {
    public record Pos(double x, double y) {}

    public PositionSetting(String name, String description, double x, double y) {
        super(name, description, new Pos(x, y));
    }

    public double x() { return value.x(); }
    public double y() { return value.y(); }

    public void setPixels(double px, double py, int screenW, int screenH) {
        if (screenW <= 0 || screenH <= 0) return;
        set(new Pos(px / screenW, py / screenH));
    }

    public double pixelX(int screenW) { return value.x() * screenW; }
    public double pixelY(int screenH) { return value.y() * screenH; }

    @Override
    protected Pos sanitize(Pos p) {
        if (p == null || Double.isNaN(p.x()) || Double.isNaN(p.y())) return null;
        return new Pos(Math.max(0, Math.min(1, p.x())), Math.max(0, Math.min(1, p.y())));
    }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("x", value.x());
        o.addProperty("y", value.y());
        return o;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (e instanceof JsonObject o && o.has("x") && o.has("y")) {
            try {
                set(new Pos(o.get("x").getAsDouble(), o.get("y").getAsDouble()));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
