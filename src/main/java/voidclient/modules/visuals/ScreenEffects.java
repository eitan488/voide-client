package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

/** CRT / VHS / pixel screen effects in the uber shader. */
public class ScreenEffects extends Module {
    public static ScreenEffects INSTANCE;

    public final ModeSetting preset = add(new ModeSetting("Preset", "Load a preset (sliders stay editable).", "Custom", "Custom", "CRT", "VHS", "Pixel", "Arcade"));
    public final NumberSetting scanlines = add(new NumberSetting("Scanlines", "Scanline darkness.", 0.25, 0, 1, 0.01));
    public final NumberSetting scanlineSize = add(new NumberSetting("Scanline Size", "Pixels per scanline.", 2, 1, 8, 0.5).unit("px"));
    public final NumberSetting curvature = add(new NumberSetting("Curvature", "CRT barrel distortion.", 0, 0, 1, 0.01));
    public final NumberSetting pixelate = add(new NumberSetting("Pixelate", "Pixel block size (1 = off).", 1, 1, 16, 1).unit("px"));
    public final NumberSetting rgbMask = add(new NumberSetting("RGB Mask", "Aperture-grille sub-pixel mask.", 0, 0, 1, 0.01));
    public final NumberSetting flicker = add(new NumberSetting("Flicker", "Brightness flicker.", 0, 0, 1, 0.01));

    public ScreenEffects() {
        super("Screen Effects", "CRT scanlines, curvature, pixelation, RGB mask and flicker.", Category.VISUALS);
        INSTANCE = this;
        preset.onChange(p -> {
            double[] v = switch (p) {
                case "CRT" -> new double[]{0.35, 2, 0.35, 1, 0.35, 0.15};
                case "VHS" -> new double[]{0.15, 3, 0.1, 1, 0, 0.5};
                case "Pixel" -> new double[]{0, 2, 0, 4, 0, 0};
                case "Arcade" -> new double[]{0.45, 2, 0.5, 2, 0.5, 0.1};
                default -> null;
            };
            if (v == null) return;
            scanlines.set(v[0]);
            scanlineSize.set(v[1]);
            curvature.set(v[2]);
            pixelate.set(v[3]);
            rgbMask.set(v[4]);
            flicker.set(v[5]);
        });
    }

    @Override
    public String getInfo() { return preset.get(); }
}
