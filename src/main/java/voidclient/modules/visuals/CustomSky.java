package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.VisualOverrides;
import voidclient.utilities.ColorUtil;

/** Sky, sunrise and cloud colours, cloud height and star brightness. */
public class CustomSky extends Module {
    public static CustomSky INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Static colour, animated rainbow or the theme accent.", "Static", "Static", "Rainbow", "Theme"));
    public final ColorSetting skyColor = add(new ColorSetting("Sky Color", "Sky colour.", 0xFF2A0F5C, false).visibleWhen(() -> mode.is("Static")));
    public final NumberSetting blend = add(new NumberSetting("Blend", "How much the custom sky replaces the biome sky.", 0.85, 0, 1, 0.05));
    public final BooleanSetting sunrise = add(new BooleanSetting("Custom Sunrise", "Recolour sunrise and sunset.", true));
    public final ColorSetting sunriseColor = add(new ColorSetting("Sunrise Color", "Sunrise/sunset glow colour.", 0xFFFF2BD6, false).visibleWhen(sunrise::get));
    public final BooleanSetting clouds = add(new BooleanSetting("Custom Clouds", "Recolour clouds.", true));
    public final ColorSetting cloudColorValue = add(new ColorSetting("Cloud Color", "Cloud colour.", 0xFFD7C9FF, false).visibleWhen(clouds::get));
    public final NumberSetting cloudHeight = add(new NumberSetting("Cloud Height Offset", "Raise or lower clouds.", 0, -64, 128, 1).unit("m"));
    public final NumberSetting starBrightness = add(new NumberSetting("Star Brightness", "Star brightness multiplier.", 1.5, 0, 4, 0.1).unit("x"));

    public CustomSky() {
        super("Custom Sky", "Recolour the sky, sunrise and clouds; tweak clouds and stars.", Category.VISUALS);
        INSTANCE = this;
    }

    private int target() {
        return switch (mode.get()) {
            case "Rainbow" -> ColorUtil.chroma(0, 0.7f, 0.6f);
            case "Theme" -> ColorUtil.darken(ThemeManager.accent(0), 0.55f);
            default -> skyColor.getColor();
        };
    }

    public int skyColor(int v) { return VisualOverrides.blendRgb(v, target(), blend.getFloat()); }
    public int sunriseColor(int v) { return sunrise.get() ? (v & 0xFF000000) | (VisualOverrides.blendRgb(v, sunriseColor.getColor(), blend.getFloat())) : v; }
    public int cloudColor(int v) { return clouds.get() ? (v & 0xFF000000) | VisualOverrides.blendRgb(v, cloudColorValue.getColor(), blend.getFloat()) : v; }
}
