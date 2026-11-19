package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

/** Cinematic colour grading in the uber shader. Presets write the sliders. */
public class ColorGrading extends Module {
    public static ColorGrading INSTANCE;

    public final ModeSetting preset = add(new ModeSetting("Preset", "Load a grading preset (sliders stay editable).", "Custom",
            "Custom", "Neutral", "Cinematic", "Cyberpunk", "Warm", "Cold", "Noir", "Vivid"));
    public final NumberSetting exposure = add(new NumberSetting("Exposure", "Exposure in stops.", 0, -2, 2, 0.05));
    public final NumberSetting contrast = add(new NumberSetting("Contrast", "Contrast multiplier.", 1.05, 0.5, 2, 0.01));
    public final NumberSetting saturation = add(new NumberSetting("Saturation", "Colour saturation.", 1.15, 0, 2.5, 0.01));
    public final NumberSetting gamma = add(new NumberSetting("Gamma", "Gamma curve.", 1, 0.5, 2, 0.01));
    public final NumberSetting temperature = add(new NumberSetting("Temperature", "Warm (+) / cool (-) white balance.", 0, -1, 1, 0.02));
    public final NumberSetting tint = add(new NumberSetting("Tint", "Green (+) / magenta (-) shift.", 0, -1, 1, 0.02));
    public final NumberSetting hueShift = add(new NumberSetting("Hue Shift", "Rotate all hues.", 0, -180, 180, 1).unit("°"));
    public final NumberSetting vibrance = add(new NumberSetting("Vibrance", "Saturates muted colours more than saturated ones.", 0.15, -1, 1, 0.01));

    public ColorGrading() {
        super("Color Grading", "Exposure, contrast, saturation, gamma, white balance and hue grading.", Category.VISUALS);
        INSTANCE = this;
        preset.onChange(this::applyPreset);
    }

    private void applyPreset(String p) {
        double[] v = switch (p) {
            case "Neutral" -> new double[]{0, 1, 1, 1, 0, 0, 0, 0};
            case "Cinematic" -> new double[]{-0.1, 1.15, 0.9, 1.05, 0.15, -0.05, 0, 0.2};
            case "Cyberpunk" -> new double[]{0.05, 1.2, 1.3, 1, -0.35, -0.3, -12, 0.35};
            case "Warm" -> new double[]{0.05, 1.05, 1.1, 1, 0.45, 0.05, 0, 0.1};
            case "Cold" -> new double[]{0, 1.05, 0.95, 1, -0.5, 0, 0, 0.05};
            case "Noir" -> new double[]{0, 1.35, 0, 1.1, 0, 0, 0, 0};
            case "Vivid" -> new double[]{0.1, 1.15, 1.45, 1, 0.05, 0, 0, 0.4};
            default -> null;
        };
        if (v == null) return;
        exposure.set(v[0]);
        contrast.set(v[1]);
        saturation.set(v[2]);
        gamma.set(v[3]);
        temperature.set(v[4]);
        tint.set(v[5]);
        hueShift.set(v[6]);
        vibrance.set(v[7]);
    }

    @Override
    public String getInfo() { return preset.get(); }
}
