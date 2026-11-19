package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.VisualOverrides;

/** Underwater overlay removal and water fog control. */
public class CustomWaterVisuals extends Module {
    public static CustomWaterVisuals INSTANCE;

    public final BooleanSetting hideOverlay = add(new BooleanSetting("Hide Overlay", "Remove the underwater screen texture.", true));
    public final NumberSetting fogDistance = add(new NumberSetting("Fog Distance", "Multiplier for underwater visibility.", 2, 0.5, 10, 0.1).unit("x"));
    public final BooleanSetting customColor = add(new BooleanSetting("Custom Fog Color", "Override the water fog colour.", false));
    public final ColorSetting fogColorValue = add(new ColorSetting("Fog Color", "Water fog colour.", 0xFF0A3A6A, false).visibleWhen(customColor::get));
    public final NumberSetting colorBlend = add(new NumberSetting("Color Blend", "How much the custom colour replaces the biome colour.", 0.7, 0, 1, 0.05).visibleWhen(customColor::get));

    public CustomWaterVisuals() {
        super("Custom Water Visuals", "Clearer water: overlay removal, fog distance and colour.", Category.VISUALS);
        INSTANCE = this;
    }

    public static boolean hidesOverlay() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideOverlay.get(); }

    public int fogColor(int v) { return customColor.get() ? VisualOverrides.blendRgb(v, fogColorValue.getColor(), colorBlend.getFloat()) : v; }
    public float fogStart(float v) { return v * fogDistance.getFloat(); }
    public float fogEnd(float v) { return v * fogDistance.getFloat(); }
}
