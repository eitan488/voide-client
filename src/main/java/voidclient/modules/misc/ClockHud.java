package voidclient.modules.misc;

import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Real-world clock, date and in-game time. */
public class ClockHud extends TextHudModule {
    public final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour.", "24h", "24h", "12h"));
    public final BooleanSetting seconds = add(new BooleanSetting("Seconds", "Show seconds.", false));
    public final BooleanSetting date = add(new BooleanSetting("Date", "Show the date.", false));
    public final BooleanSetting gameTime = add(new BooleanSetting("Game Time", "In-game clock.", true));

    public ClockHud() {
        super("Clock HUD", "Real time, date and in-game time.", Category.MISC, 0.005, 0.25);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        String pattern = (format.is("12h") ? "h:mm" : "HH:mm") + (seconds.get() ? ":ss" : "") + (format.is("12h") ? " a" : "");
        LocalDateTime now = LocalDateTime.now();
        out.add(new Line("Time", now.format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT))));
        if (date.get()) out.add(new Line("Date", now.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ROOT))));
        if (gameTime.get() && mc.world != null) {
            long tod = (mc.world.getTimeOfDay() + 6000) % 24000;
            out.add(new Line("Game", String.format(Locale.ROOT, "%02d:%02d", tod / 1000, (tod % 1000) * 60 / 1000)));
        }
    }
}
