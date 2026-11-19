package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.Setting;
import voidclient.gui.screens.ClickGuiScreen;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;

/**
 * Theme selection (Cyberpunk, Galaxy, Minimal, Plasma, AMOLED) and the Custom Theme Creator.
 * Always active; saved per profile.
 */
public class ThemeEngine extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", "Client colour theme.", "Cyberpunk", "Cyberpunk", "Galaxy", "Minimal", "Plasma", "AMOLED", "Custom"));
    public final BooleanSetting animated = add(new BooleanSetting("Animated Gradient", "Accent colours flow through the gradient.", true));
    public final NumberSetting speed = add(new NumberSetting("Gradient Speed", "Speed of the accent gradient.", 1, 0.1, 4, 0.1).unit("x"));
    public final NumberSetting glow = add(new NumberSetting("Glow Strength", "Neon glow around panels and toggles.", 1, 0, 2, 0.05));
    public final NumberSetting opacity = add(new NumberSetting("Panel Opacity", "Glass panel opacity.", 1, 0.4, 1, 0.02));
    public final ColorSetting background = add(new ColorSetting("Background", "Custom theme background.", 0xF2050510).visibleWhen(this::custom));
    public final ColorSetting surface = add(new ColorSetting("Surface", "Custom theme panel/card colour.", 0xD90E0E1E).visibleWhen(this::custom));
    public final ColorSetting accent1 = add(new ColorSetting("Accent 1", "Custom primary accent.", 0xFFB026FF, false).visibleWhen(this::custom));
    public final ColorSetting accent2 = add(new ColorSetting("Accent 2", "Custom secondary accent.", 0xFF00F0FF, false).visibleWhen(this::custom));
    public final ColorSetting accent3 = add(new ColorSetting("Accent 3", "Custom tertiary accent.", 0xFFFF2BD6, false).visibleWhen(this::custom));
    public final ColorSetting text = add(new ColorSetting("Text", "Custom text colour.", 0xFFF4F1FF, false).visibleWhen(this::custom));

    public ThemeEngine() {
        super("Theme Engine", "Cyberpunk, Galaxy, Minimal, Plasma and AMOLED themes plus a custom theme creator.", Category.VISUALS);
        for (Setting<?> s : getSettings()) s.onChange(v -> apply());
        apply();
    }

    private boolean custom() { return theme.is("Custom"); }

    public void apply() {
        Theme t = switch (theme.get()) {
            case "Galaxy" -> Theme.GALAXY;
            case "Minimal" -> Theme.MINIMAL;
            case "Plasma" -> Theme.PLASMA;
            case "AMOLED" -> Theme.AMOLED;
            case "Custom" -> new Theme("Custom", background.getColor(), surface.getColor(),
                    voidclient.utilities.ColorUtil.brighten(surface.getColor(), 0.08f),
                    voidclient.utilities.ColorUtil.withAlpha(accent1.getColor(), 64), accent1.getColor(), accent2.getColor(), accent3.getColor(),
                    text.getColor(), voidclient.utilities.ColorUtil.multiplyAlpha(text.getColor(), 0.55f), 0xFF3CFFA0, 0xFFFFC93C, 0xFFFF3C6E);
            default -> Theme.CYBERPUNK;
        };
        ThemeManager.apply(t, animated.get(), speed.getFloat(), glow.getFloat(), opacity.getFloat());
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() { mc.setScreen(new ClickGuiScreen(mc.currentScreen, Category.SETTINGS)); }

    @Override
    public String getInfo() { return theme.get(); }
}
