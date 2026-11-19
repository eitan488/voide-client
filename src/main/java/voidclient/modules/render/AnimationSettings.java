package voidclient.modules.render;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.Setting;
import voidclient.gui.animations.AnimationManager;
import voidclient.gui.animations.Easing;

/** GUI animation behaviour and first-person swing speed. Always applied. */
public class AnimationSettings extends Module {
    public static AnimationSettings INSTANCE;

    public final BooleanSetting guiAnimations = add(new BooleanSetting("GUI Animations", "Animate the Void GUI (off = instant).", true));
    public final NumberSetting speed = add(new NumberSetting("Animation Speed", "Speed multiplier for every GUI animation.", 1, 0.25, 3, 0.05).unit("x"));
    public final ModeSetting easing = add(new ModeSetting("Easing", "Default easing curve.", "Cubic", "Linear", "Sine", "Quad", "Cubic", "Quint", "Expo", "Back"));
    public final BooleanSetting customSwing = add(new BooleanSetting("Custom Swing Speed", "Change your hand swing animation length.", false));
    public final NumberSetting swing = add(new NumberSetting("Swing Duration", "Swing length in ticks (vanilla 6).", 6, 2, 20, 1).visibleWhen(customSwing::get));

    public AnimationSettings() {
        super("Animation Settings", "GUI animation speed and easing, plus hand swing speed.", Category.RENDER);
        INSTANCE = this;
        for (Setting<?> s : getSettings()) s.onChange(v -> apply());
        apply();
    }

    public void apply() {
        Easing e = switch (easing.get()) {
            case "Linear" -> Easing.LINEAR;
            case "Sine" -> Easing.SINE_OUT;
            case "Quad" -> Easing.QUAD_OUT;
            case "Quint" -> Easing.QUINT_OUT;
            case "Expo" -> Easing.EXPO_OUT;
            case "Back" -> Easing.BACK_OUT;
            default -> Easing.CUBIC_OUT;
        };
        AnimationManager.configure(guiAnimations.get(), speed.get(), e);
    }

    public static int swingDuration(int original) {
        AnimationSettings a = INSTANCE;
        if (a == null || !a.customSwing.get()) return original;
        return a.swing.getInt();
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() {
        mc.setScreen(new voidclient.gui.screens.ClickGuiScreen(mc.currentScreen));
    }
}
