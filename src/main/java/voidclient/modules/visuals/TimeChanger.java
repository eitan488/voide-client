package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

/** Client-side visual time of day (sky, sun, light); the server time is untouched. */
public class TimeChanger extends Module {
    public static TimeChanger INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Mode", "Fixed time, a fast cycle, or an offset from real time.", "Fixed", "Fixed", "Cycle", "Offset"));
    public final NumberSetting time = add(new NumberSetting("Time", "Time of day in ticks (0 = sunrise, 6000 = noon, 18000 = midnight).", 18000, 0, 23999, 50).visibleWhen(() -> mode.is("Fixed")));
    public final NumberSetting cycleSpeed = add(new NumberSetting("Cycle Speed", "Ticks of sky time per real second.", 400, 20, 4000, 20).visibleWhen(() -> mode.is("Cycle")));
    public final NumberSetting offset = add(new NumberSetting("Offset", "Hours added to the real time.", 6, -12, 12, 0.5).unit("h").visibleWhen(() -> mode.is("Offset")));

    public TimeChanger() {
        super("Time Changer", "Change the visual time of day client-side.", Category.VISUALS);
        INSTANCE = this;
    }

    public static long modify(long original) {
        TimeChanger t = INSTANCE;
        if (t == null || !t.isEnabled()) return original;
        long day = Math.floorDiv(original, 24000L) * 24000L;
        return switch (t.mode.get()) {
            case "Cycle" -> day + (long) ((System.currentTimeMillis() / 1000.0 * t.cycleSpeed.get()) % 24000);
            case "Offset" -> original + (long) (t.offset.get() * 1000);
            default -> day + t.time.getInt();
        };
    }

    @Override
    public String getInfo() { return mode.is("Fixed") ? String.valueOf(time.getInt()) : mode.get(); }
}
