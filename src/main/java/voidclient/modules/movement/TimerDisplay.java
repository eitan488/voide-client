package voidclient.modules.movement;

import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.keybinds.Keybind;
import voidclient.utilities.Sounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stopwatch / countdown with start-stop, lap and reset keys. */
public class TimerDisplay extends TextHudModule {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Stopwatch counts up, countdown counts down.", "Stopwatch", "Stopwatch", "Countdown"));
    public final NumberSetting countdown = add(new NumberSetting("Countdown", "Countdown length.", 300, 5, 7200, 5).unit("s").visibleWhen(() -> mode.is("Countdown")));
    public final KeybindSetting startStop = add(new KeybindSetting("Start/Stop Key", "Start or pause the timer.", Keybind.NONE));
    public final KeybindSetting lapKey = add(new KeybindSetting("Lap Key", "Record a lap split.", Keybind.NONE));
    public final KeybindSetting resetKey = add(new KeybindSetting("Reset Key", "Reset the timer.", Keybind.NONE));
    public final BooleanSetting millis = add(new BooleanSetting("Milliseconds", "Show hundredths.", true));
    public final NumberSetting laps = add(new NumberSetting("Laps Shown", "Number of recent laps listed.", 3, 0, 10, 1));
    public final SoundSetting alarm = add(new SoundSetting("Alarm", "Sound when a countdown ends.", Sounds.ClientSound.NOTIFICATION, true, 0.9));

    private long accumulated, startedAt;
    private boolean running, fired;
    private final List<Long> lapTimes = new ArrayList<>();

    public TimerDisplay() {
        super("Timer Display", "Stopwatch and countdown timer with lap splits.", Category.MOVEMENT, 0.40, 0.02);
        on(Events.Key.class, e -> {
            if (e.action != 1 || mc.currentScreen != null) return;
            if (startStop.get().matchesKey(e.key)) toggleRun();
            else if (lapKey.get().matchesKey(e.key) && running) lapTimes.add(0, elapsed());
            else if (resetKey.get().matchesKey(e.key)) reset();
        });
        on(Events.TickPost.class, e -> {
            if (mode.is("Countdown") && running && remaining() <= 0 && !fired) {
                fired = true;
                running = false;
                accumulated = (long) (countdown.get() * 1000);
                alarm.play();
                voidclient.gui.Notifications.info("Timer", "Countdown finished");
            }
        });
    }

    private void toggleRun() {
        if (running) {
            accumulated += System.currentTimeMillis() - startedAt;
            running = false;
        } else {
            startedAt = System.currentTimeMillis();
            running = true;
            fired = false;
        }
    }

    private void reset() {
        running = false;
        accumulated = 0;
        fired = false;
        lapTimes.clear();
    }

    private long elapsed() { return accumulated + (running ? System.currentTimeMillis() - startedAt : 0); }

    private long remaining() { return (long) (countdown.get() * 1000) - elapsed(); }

    private String fmt(long ms) {
        ms = Math.max(0, ms);
        long h = ms / 3_600_000, m = ms / 60_000 % 60, s = ms / 1000 % 60, cs = ms / 10 % 100;
        String base = h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%02d:%02d", m, s);
        return millis.get() ? base + String.format(Locale.ROOT, ".%02d", cs) : base;
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        long shown = mode.is("Countdown") ? remaining() : elapsed();
        out.add(new Line(running ? "▶" : "⏸", fmt(shown), running ? accent() : 0));
        for (int i = 0; i < Math.min(laps.getInt(), lapTimes.size()); i++) out.add(new Line("Lap " + (lapTimes.size() - i), fmt(lapTimes.get(i))));
    }
}
