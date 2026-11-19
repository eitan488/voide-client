package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

/** Client-side visual weather override. */
public class WeatherChanger extends Module {
    public static WeatherChanger INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Weather", "Visual weather to show.", "Clear", "Clear", "Rain", "Thunder", "Custom"));
    public final NumberSetting rain = add(new NumberSetting("Rain Strength", "Rain gradient.", 1, 0, 1, 0.05).visibleWhen(() -> mode.is("Custom")));
    public final NumberSetting thunder = add(new NumberSetting("Thunder Strength", "Thunder gradient (darkens the sky).", 0, 0, 1, 0.05).visibleWhen(() -> mode.is("Custom")));

    public WeatherChanger() {
        super("Weather Changer", "Change the visual weather client-side.", Category.VISUALS);
        INSTANCE = this;
    }

    public static float rain(float original) {
        WeatherChanger w = INSTANCE;
        if (w == null || !w.isEnabled()) return original;
        return switch (w.mode.get()) {
            case "Clear" -> 0f;
            case "Rain", "Thunder" -> 1f;
            default -> w.rain.getFloat();
        };
    }

    public static float thunder(float original) {
        WeatherChanger w = INSTANCE;
        if (w == null || !w.isEnabled()) return original;
        return switch (w.mode.get()) {
            case "Thunder" -> 1f;
            case "Custom" -> w.thunder.getFloat();
            default -> 0f;
        };
    }

    @Override
    public String getInfo() { return mode.get(); }
}
