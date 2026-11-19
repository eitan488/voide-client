package voidclient.modules.render;

import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

import java.util.List;

/** Frames per second with colour thresholds, min/max and frame time. */
public class FpsHud extends TextHudModule {
    public final BooleanSetting colorByFps = add(new BooleanSetting("Color By FPS", "Green/yellow/red by thresholds.", true));
    public final NumberSetting good = add(new NumberSetting("Good FPS", "At or above this is green.", 120, 30, 360, 5).visibleWhen(colorByFps::get));
    public final NumberSetting bad = add(new NumberSetting("Bad FPS", "Below this is red.", 45, 10, 120, 5).visibleWhen(colorByFps::get));
    public final BooleanSetting minMax = add(new BooleanSetting("Min / Max", "Lowest and highest FPS of the last 10 seconds.", false));
    public final BooleanSetting frameTime = add(new BooleanSetting("Frame Time", "Average frame time in ms.", false));

    private final int[] samples = new int[200];
    private int head, count;

    public FpsHud() {
        super("FPS HUD", "Frames per second with colour coding.", Category.RENDER, 0.005, 0.075);
        on(Events.TickPost.class, e -> {
            samples[head] = mc.getCurrentFps();
            head = (head + 1) % samples.length;
            count = Math.min(count + 1, samples.length);
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        int fps = mc.getCurrentFps();
        int c = !colorByFps.get() ? 0 : fps >= good.get() ? 0xFF3CFFA0 : fps < bad.get() ? 0xFFFF3C6E : 0xFFFFC93C;
        out.add(new Line("FPS", String.valueOf(fps), c));
        if (minMax.get() && count > 0) {
            int min = Integer.MAX_VALUE, max = 0;
            for (int i = 0; i < count; i++) {
                min = Math.min(min, samples[i]);
                max = Math.max(max, samples[i]);
            }
            out.add(new Line("Min/Max", min + " / " + max));
        }
        if (frameTime.get()) out.add(new Line("Frame", String.format(java.util.Locale.ROOT, "%.1f ms", 1000.0 / Math.max(1, fps))));
    }
}
