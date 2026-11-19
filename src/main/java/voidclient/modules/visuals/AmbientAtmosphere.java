package voidclient.modules.visuals;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.VisualOverrides;

/** One-click moods combining fog, sky and light colour. */
public class AmbientAtmosphere extends Module {
    public static AmbientAtmosphere INSTANCE;

    public final ModeSetting mood = add(new ModeSetting("Mood", "Atmosphere preset.", "Neon Night", "Dusk", "Neon Night", "Haze", "Abyss", "Custom"));
    public final NumberSetting intensity = add(new NumberSetting("Intensity", "How strongly the mood is applied.", 0.6, 0, 1, 0.05));
    public final NumberSetting fogDensity = add(new NumberSetting("Fog Density", "Pulls fog closer (1 = unchanged).", 1, 0.3, 1.5, 0.05));
    public final ColorSetting customFog = add(new ColorSetting("Custom Fog", "Fog colour for Custom mood.", 0xFF301050, false).visibleWhen(() -> mood.is("Custom")));
    public final ColorSetting customSky = add(new ColorSetting("Custom Sky", "Sky colour for Custom mood.", 0xFF14082E, false).visibleWhen(() -> mood.is("Custom")));
    public final ColorSetting customLight = add(new ColorSetting("Custom Light", "Light tint for Custom mood.", 0xFFB49CFF, false).visibleWhen(() -> mood.is("Custom")));

    public AmbientAtmosphere() {
        super("Ambient Atmosphere", "Mood presets that tint fog, sky and light together.", Category.VISUALS);
        INSTANCE = this;
    }

    private int[] palette() {
        return switch (mood.get()) {
            case "Dusk" -> new int[]{0xFFB0566B, 0xFF6A2F5C, 0xFFFFB38A};
            case "Haze" -> new int[]{0xFF9AA0B8, 0xFF8890A8, 0xFFE0E4F0};
            case "Abyss" -> new int[]{0xFF050A1A, 0xFF02040C, 0xFF6C7CFF};
            case "Custom" -> new int[]{customFog.getColor(), customSky.getColor(), customLight.getColor()};
            default -> new int[]{0xFF2A0F55, 0xFF120630, 0xFFB026FF};
        };
    }

    public Object apply(EnvironmentAttribute<?> attr, Object v) {
        int[] p = palette();
        float k = intensity.getFloat();
        if (attr == EnvironmentAttributes.FOG_COLOR_VISUAL) return VisualOverrides.blendRgb((Integer) v, p[0], k);
        if (attr == EnvironmentAttributes.SKY_COLOR_VISUAL) return VisualOverrides.blendRgb((Integer) v, p[1], k);
        if (attr == EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL) return VisualOverrides.blendRgb((Integer) v, p[2], k * 0.5f);
        if (attr == EnvironmentAttributes.FOG_END_DISTANCE_VISUAL) return (Float) v * fogDensity.getFloat();
        if (attr == EnvironmentAttributes.FOG_START_DISTANCE_VISUAL) return (Float) v * fogDensity.getFloat();
        return v;
    }

    @Override
    public String getInfo() { return mood.get(); }
}
