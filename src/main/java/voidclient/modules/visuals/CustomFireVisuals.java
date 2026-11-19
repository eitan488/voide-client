package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;

/** Lower, fade, tint or hide the first-person fire overlay. */
public class CustomFireVisuals extends Module {
    public static CustomFireVisuals INSTANCE;

    public final BooleanSetting hide = add(new BooleanSetting("Hide Overlay", "Remove the fire overlay entirely.", false));
    public final NumberSetting lower = add(new NumberSetting("Lower By", "Move the flames down the screen.", 0.25, 0, 0.6, 0.01).visibleWhen(() -> !hide.get()));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Flame opacity.", 0.6, 0.05, 1, 0.05).visibleWhen(() -> !hide.get()));
    public final ColorSetting tint = add(new ColorSetting("Tint", "Flame colour multiplier.", 0xFFFFFFFF, false).visibleWhen(() -> !hide.get()));

    public CustomFireVisuals() {
        super("Custom Fire Visuals", "Make the on-fire overlay smaller, fainter, tinted or hidden.", Category.VISUALS);
        INSTANCE = this;
    }

    public static boolean active() { return INSTANCE != null && INSTANCE.isEnabled(); }
    public static boolean hidden() { return INSTANCE.hide.get(); }
    public static float lowerBy() { return INSTANCE.lower.getFloat(); }
    public static float opacity() { return INSTANCE.opacity.getFloat(); }
    public static int tint() { return INSTANCE.tint.getColor(); }
}
