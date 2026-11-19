package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;

public class Vignette extends Module {
    public static Vignette INSTANCE;

    public final NumberSetting intensity = add(new NumberSetting("Intensity", "Darkening strength at the edges.", 0.55, 0, 1, 0.01));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Where the falloff ends (1 = corners).", 1.05, 0.3, 1.5, 0.01));
    public final NumberSetting softness = add(new NumberSetting("Softness", "Width of the falloff.", 0.55, 0.05, 1, 0.01));
    public final NumberSetting roundness = add(new NumberSetting("Roundness", "1 = circular, 0 = follows screen aspect.", 1, 0, 1, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", "Vignette colour.", 0xFF000000, false));

    public Vignette() {
        super("Vignette", "Shader vignette with radius, softness, roundness and colour.", Category.VISUALS);
        INSTANCE = this;
    }
}
