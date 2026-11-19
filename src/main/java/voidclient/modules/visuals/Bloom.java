package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

/** Real multi-pass bloom: threshold extract, separable gaussian blur, additive composite. */
public class Bloom extends Module {
    public static Bloom INSTANCE;

    public final NumberSetting intensity = add(new NumberSetting("Intensity", "Strength of the glow added back to the image.", 0.8, 0, 3, 0.05));
    public final NumberSetting threshold = add(new NumberSetting("Threshold", "Brightness above which pixels bloom.", 0.65, 0, 1, 0.01));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Blur spread in texels of the bloom buffer.", 1.5, 0.5, 4, 0.1));
    public final ModeSetting quality = add(new ModeSetting("Quality", "Bloom buffer resolution and blur iterations.", "Medium", "Low", "Medium", "High", "Ultra"));
    public final ColorSetting glowColor = add(new ColorSetting("Glow Color", "Tint applied to the bloom.", 0xFFB026FF, false).themed());
    public final BooleanSetting customColor = add(new BooleanSetting("Custom Color", "Tint the bloom with Glow Color instead of the scene colour.", false));
    public final NumberSetting colorMix = add(new NumberSetting("Color Mix", "How strongly Glow Color replaces scene colour.", 0.5, 0, 1, 0.05).visibleWhen(customColor::get));
    public final NumberSetting saturation = add(new NumberSetting("Saturation Boost", "Extra saturation in the bloom.", 0.3, -1, 2, 0.05));
    public final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Breathing pulse of the intensity (0 = static).", 0, 0, 3, 0.1));
    public final BooleanSetting performance = add(new BooleanSetting("Performance Mode", "Quarter-resolution single-pass bloom.", false));

    public Bloom() {
        super("Bloom", "Neon bloom glow on bright pixels using a real multi-pass shader.", Category.VISUALS);
        INSTANCE = this;
    }

    public int downscale() {
        if (performance.get()) return 4;
        return switch (quality.get()) {
            case "Low" -> 8;
            case "High" -> 2;
            case "Ultra" -> 1;
            default -> 4;
        };
    }

    public int iterations() {
        if (performance.get()) return 1;
        return switch (quality.get()) {
            case "Low" -> 1;
            case "High" -> 3;
            case "Ultra" -> 4;
            default -> 2;
        };
    }

    public double intensity() {
        double s = animationSpeed.get();
        if (s <= 0) return intensity.get();
        return intensity.get() * (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * s));
    }

    public double threshold() { return threshold.get(); }
    public double radius() { return radius.get(); }
    public double saturation() { return saturation.get(); }
    public int glowColor() { return glowColor.getColor(); }
    public double tintMix() { return customColor.get() ? colorMix.get() : 0; }

    @Override
    public String getInfo() { return quality.get(); }

}
