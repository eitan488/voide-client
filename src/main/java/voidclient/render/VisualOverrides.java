package voidclient.render;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import voidclient.modules.visuals.AmbientAtmosphere;
import voidclient.modules.visuals.CustomFog;
import voidclient.modules.visuals.CustomLighting;
import voidclient.modules.visuals.CustomSky;
import voidclient.modules.visuals.CustomWaterVisuals;
import voidclient.utilities.ColorUtil;

/**
 * Applies Void visual modules to 1.21.11 environment attributes (sky, fog, cloud, light).
 * Colour attributes are RGB ints; distance attributes are floats in blocks.
 */
public final class VisualOverrides {
    private VisualOverrides() {}

    @SuppressWarnings("unchecked")
    public static <V> V apply(EnvironmentAttribute<V> attr, V value) {
        Object v = value;
        AmbientAtmosphere atmo = AmbientAtmosphere.INSTANCE;
        if (atmo != null && atmo.isEnabled()) v = atmo.apply(attr, v);
        CustomSky sky = CustomSky.INSTANCE;
        if (sky != null && sky.isEnabled()) {
            if (attr == EnvironmentAttributes.SKY_COLOR_VISUAL) v = sky.skyColor((Integer) v);
            else if (attr == EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL) v = sky.sunriseColor((Integer) v);
            else if (attr == EnvironmentAttributes.CLOUD_COLOR_VISUAL) v = sky.cloudColor((Integer) v);
            else if (attr == EnvironmentAttributes.CLOUD_HEIGHT_VISUAL) v = (Float) v + sky.cloudHeight.getFloat();
            else if (attr == EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL) v = Math.min(1f, (Float) v * sky.starBrightness.getFloat());
        }
        CustomFog fog = CustomFog.INSTANCE;
        if (fog != null && fog.isEnabled()) {
            if (attr == EnvironmentAttributes.FOG_COLOR_VISUAL) v = fog.color((Integer) v);
            else if (attr == EnvironmentAttributes.FOG_START_DISTANCE_VISUAL) v = fog.start((Float) v);
            else if (attr == EnvironmentAttributes.FOG_END_DISTANCE_VISUAL) v = fog.end((Float) v);
            else if (attr == EnvironmentAttributes.SKY_FOG_END_DISTANCE_VISUAL) v = fog.skyEnd((Float) v);
            else if (attr == EnvironmentAttributes.CLOUD_FOG_END_DISTANCE_VISUAL) v = fog.skyEnd((Float) v);
        }
        CustomWaterVisuals water = CustomWaterVisuals.INSTANCE;
        if (water != null && water.isEnabled()) {
            if (attr == EnvironmentAttributes.WATER_FOG_COLOR_VISUAL) v = water.fogColor((Integer) v);
            else if (attr == EnvironmentAttributes.WATER_FOG_START_DISTANCE_VISUAL) v = water.fogStart((Float) v);
            else if (attr == EnvironmentAttributes.WATER_FOG_END_DISTANCE_VISUAL) v = water.fogEnd((Float) v);
        }
        CustomLighting light = CustomLighting.INSTANCE;
        if (light != null && light.isEnabled()) {
            if (attr == EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL) v = light.skyLightColor((Integer) v);
            else if (attr == EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL) v = light.skyLightFactor((Float) v);
        }
        return (V) v;
    }

    /** Blend helper for RGB attribute colours (alpha ignored by the game). */
    public static int blendRgb(int original, int target, float amount) {
        return ColorUtil.lerp(original | 0xFF000000, target | 0xFF000000, amount) & 0xFFFFFF;
    }
}
