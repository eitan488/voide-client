package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.VisualOverrides;

/** Sky light tint and daylight factor (affects the lightmap of the whole world). */
public class CustomLighting extends Module {
    public static CustomLighting INSTANCE;

    public final ColorSetting tint = add(new ColorSetting("Light Tint", "Sky light colour.", 0xFFC8B4FF, false));
    public final NumberSetting tintStrength = add(new NumberSetting("Tint Strength", "Amount of tint.", 0.35, 0, 1, 0.05));
    public final NumberSetting brightness = add(new NumberSetting("Daylight Factor", "Sky light multiplier.", 1, 0.2, 2, 0.05).unit("x"));
    public final NumberSetting nightFloor = add(new NumberSetting("Night Floor", "Minimum sky light at night.", 0, 0, 1, 0.05));

    public CustomLighting() {
        super("Custom Lighting", "Tint and scale world sky light.", Category.VISUALS);
        INSTANCE = this;
    }

    public int skyLightColor(int v) { return VisualOverrides.blendRgb(v, tint.getColor(), tintStrength.getFloat()); }
    public float skyLightFactor(float v) { return Math.max(nightFloor.getFloat(), Math.min(1.5f, v * brightness.getFloat())); }
}
