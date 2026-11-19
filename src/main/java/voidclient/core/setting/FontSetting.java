package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.render.FontRenderer;

/** Font family + shadow choice for text-rendering elements. */
public class FontSetting extends Setting<FontSetting.FontValue> {
    public record FontValue(FontRenderer.Family family, boolean shadow) {}

    public FontSetting(String name, String description, FontRenderer.Family family, boolean shadow) {
        super(name, description, new FontValue(family, shadow));
    }

    public FontRenderer.Family family() { return value.family(); }
    public boolean shadow() { return value.shadow(); }

    public void setFamily(FontRenderer.Family f) { set(new FontValue(f, value.shadow())); }
    public void setShadow(boolean s) { set(new FontValue(value.family(), s)); }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("family", value.family().name());
        o.addProperty("shadow", value.shadow());
        return o;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (!(e instanceof JsonObject o)) return;
        try {
            FontRenderer.Family f = o.has("family") ? FontRenderer.Family.valueOf(o.get("family").getAsString()) : value.family();
            boolean s = o.has("shadow") ? o.get("shadow").getAsBoolean() : value.shadow();
            set(new FontValue(f, s));
        } catch (RuntimeException ignored) {
        }
    }
}
