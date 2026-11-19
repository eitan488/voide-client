package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;

public class ScreenTint extends Module {
    public static ScreenTint INSTANCE;

    public final ColorSetting color = add(new ColorSetting("Color", "Tint colour (supports chroma and theme).", 0xFFB026FF, false));
    public final NumberSetting strength = add(new NumberSetting("Strength", "Tint amount.", 0.2, 0, 1, 0.01));

    public ScreenTint() {
        super("Screen Tint", "Colour-tints the whole game image.", Category.VISUALS);
        INSTANCE = this;
    }
}
