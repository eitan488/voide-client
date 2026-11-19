package voidclient.modules.utility;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.events.Events;
import voidclient.particles.ParticleManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Frame-time graph, FPS percentiles, memory, entities and particles. */
public class PerformanceMonitor extends HudModule {
    public final BooleanSetting graph = add(new BooleanSetting("Frame Graph", "Frame time graph.", true));
    public final BooleanSetting memory = add(new BooleanSetting("Memory", "Java heap usage.", true));
    public final BooleanSetting lows = add(new BooleanSetting("1% Low", "1% low FPS from recent frames.", true));
    public final BooleanSetting worldStats = add(new BooleanSetting("World Stats", "Entities and Void particles.", true));

    private final float[] frames = new float[240];
    private int head;
    private long lastFrame = System.nanoTime();

    public PerformanceMonitor() {
        super("Performance Monitor", "Frame time graph, 1% lows, memory and world load.", Category.UTILITY, 0.005, 0.80);
        alsoListIn(Category.RENDER);
        on(Events.HudRender.class, e -> {
            long now = System.nanoTime();
            frames[head] = (now - lastFrame) / 1e6f;
            head = (head + 1) % frames.length;
            lastFrame = now;
        });
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float w = 140;
        float h = 4 + 10 + (lows.get() ? 10 : 0) + (memory.get() ? 10 : 0) + (worldStats.get() ? 10 : 0) + (graph.get() ? 34 : 0);
        size(w, h);
        panel(ctx, 0, 0, w, h);
        float avg = 0, worst = 0;
        float[] sorted = frames.clone();
        for (float f : frames) avg += f;
        avg /= frames.length;
        java.util.Arrays.sort(sorted);
        worst = sorted[(int) (sorted.length * 0.99)];
        float y = 3;
        text(ctx, String.format(Locale.ROOT, "%d FPS  %.2f ms", mc.getCurrentFps(), avg), 4, y, textColor.getColor());
        y += 10;
        if (lows.get()) {
            text(ctx, String.format(Locale.ROOT, "1%% low %.0f FPS", worst > 0 ? 1000 / worst : 0), 4, y, dim(textColor.getColor()));
            y += 10;
        }
        if (memory.get()) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576, max = rt.maxMemory() / 1048576;
            text(ctx, String.format(Locale.ROOT, "Mem %d / %d MB (%d%%)", used, max, used * 100 / Math.max(1, max)), 4, y, dim(textColor.getColor()));
            y += 10;
        }
        if (worldStats.get()) {
            int ents = mc.world == null ? 0 : (int) java.util.stream.StreamSupport.stream(mc.world.getEntities().spliterator(), false).count();
            text(ctx, "Entities " + ents + "  Void FX " + ParticleManager.get().count(), 4, y, dim(textColor.getColor()));
            y += 10;
        }
        if (graph.get()) {
            float gx = 4, gy = y + 2, gw = w - 8, gh = 28;
            Render2D.rect(ctx, gx, gy, gw, gh, 0x40000000);
            float target = 1000f / 60f;
            float ty = gy + gh - gh * Math.min(1, target / 50f);
            Render2D.rect(ctx, gx, ty, gw, 0.5f, 0x8000F0FF);
            for (int i = 0; i < frames.length; i++) {
                float f = frames[(head + i) % frames.length];
                float bh = gh * Math.min(1f, f / 50f);
                int c = f > 33 ? 0xFFFF3C6E : f > 17 ? 0xFFFFC93C : ColorUtil.withAlpha(accent(), 230);
                Render2D.rect(ctx, gx + gw * i / frames.length, gy + gh - bh, gw / frames.length + 0.2f, bh, c);
            }
        }
    }
}
