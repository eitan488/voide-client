package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.VisualOverrides;

/** Terrain and sky fog distance and colour control (removes fog in Disable mode). */
public class CustomFog extends Module {
    public static CustomFog INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Disable removes distance fog; Custom scales it.", "Custom", "Disable", "Custom"));
    public final NumberSetting start = add(new NumberSetting("Start Multiplier", "Fog start distance multiplier.", 1.5, 0, 4, 0.05).unit("x").visibleWhen(() -> mode.is("Custom")));
    public final NumberSetting end = add(new NumberSetting("End Multiplier", "Fog end distance multiplier.", 1.5, 0.2, 4, 0.05).unit("x").visibleWhen(() -> mode.is("Custom")));
    public final NumberSetting sky = add(new NumberSetting("Sky Fog Multiplier", "Sky/cloud fog distance multiplier.", 1.2, 0.2, 4, 0.05).unit("x").visibleWhen(() -> mode.is("Custom")));
    public final ColorSetting color = add(new ColorSetting("Color", "Fog colour.", 0xFF1A0F33, false));
    public final NumberSetting colorBlend = add(new NumberSetting("Color Blend", "How much the custom colour replaces the biome fog colour.", 0, 0, 1, 0.05));

    public CustomFog() {
        super("Custom Fog", "Push fog back, remove it, or recolour it.", Category.VISUALS);
        INSTANCE = this;
    }

    public int color(int v) { return VisualOverrides.blendRgb(v, color.getColor(), colorBlend.getFloat()); }
    public float start(float v) { return mode.is("Disable") ? 100_000f : v * start.getFloat(); }
    public float end(float v) { return mode.is("Disable") ? 100_001f : v * end.getFloat(); }
    public float skyEnd(float v) { return mode.is("Disable") ? 100_001f : v * sky.getFloat(); }

    @Override
    public String getInfo() { return mode.get(); }
}
