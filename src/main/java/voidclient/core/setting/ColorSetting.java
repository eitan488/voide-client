package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import voidclient.utilities.ColorUtil;

/**
 * ARGB colour with optional animated "chroma" (hue cycling) and theme-accent binding.
 * Values are packed into {@link ColorValue} so equality/serialisation stay simple.
 */
public class ColorSetting extends Setting<ColorSetting.ColorValue> {
    public record ColorValue(int argb, boolean chroma, boolean themed) {}

    private final boolean allowAlpha;

    public ColorSetting(String name, String description, int argb) {
        this(name, description, argb, true);
    }

    public ColorSetting(String name, String description, int argb, boolean allowAlpha) {
        super(name, description, new ColorValue(argb, false, false));
        this.allowAlpha = allowAlpha;
    }

    public ColorSetting themed() {
        this.defaultValue = this.value = new ColorValue(value.argb(), false, true);
        return this;
    }

    public boolean allowsAlpha() { return allowAlpha; }

    /** Raw stored colour (ignores chroma/theme). */
    public int raw() { return value.argb(); }

    /** Colour as it should be rendered right now. */
    public int getColor() { return getColor(0); }

    public int getColor(float hueOffset) {
        ColorValue v = value;
        int alpha = (v.argb() >>> 24) & 0xFF;
        if (v.themed()) return ColorUtil.withAlpha(voidclient.gui.themes.ThemeManager.accent(hueOffset), alpha);
        if (v.chroma()) return ColorUtil.withAlpha(ColorUtil.chroma(hueOffset, 0.75f, 1f), alpha);
        return v.argb();
    }

    public void setArgb(int argb) { set(new ColorValue(allowAlpha ? argb : (argb | 0xFF000000), value.chroma(), value.themed())); }
    public void setChroma(boolean chroma) { set(new ColorValue(value.argb(), chroma, chroma ? false : value.themed())); }
    public void setThemed(boolean themed) { set(new ColorValue(value.argb(), themed ? false : value.chroma(), themed)); }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("argb", String.format("#%08X", value.argb()));
        o.addProperty("chroma", value.chroma());
        o.addProperty("themed", value.themed());
        return o;
    }

    @Override
    public void fromJson(JsonElement e) {
        try {
            if (e instanceof JsonPrimitive p) {
                set(new ColorValue(ColorUtil.parseHex(p.getAsString(), value.argb()), false, false));
            } else if (e instanceof JsonObject o) {
                int argb = o.has("argb") ? ColorUtil.parseHex(o.get("argb").getAsString(), value.argb()) : value.argb();
                boolean chroma = o.has("chroma") && o.get("chroma").getAsBoolean();
                boolean themed = o.has("themed") && o.get("themed").getAsBoolean();
                set(new ColorValue(argb, chroma, themed));
            }
        } catch (RuntimeException ignored) {
            // malformed entry: keep current value
        }
    }
}
