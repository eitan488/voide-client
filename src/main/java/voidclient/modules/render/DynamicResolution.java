package voidclient.modules.render;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.RangeSetting;
import voidclient.events.Events;

/**
 * Adaptive quality for a target frame rate. Minecraft 1.21.11 renders the world at window
 * resolution, so instead of scaling the framebuffer this adapts render distance, entity
 * distance and particle density.
 */
public class DynamicResolution extends Module {
    public final NumberSetting targetFps = add(new NumberSetting("Target FPS", "Frame rate to maintain.", 90, 30, 360, 5));
    public final RangeSetting distance = add(new RangeSetting("Render Distance Range", "Render distance bounds (chunks).", 6, 16, 2, 32, 1));
    public final NumberSetting interval = add(new NumberSetting("Adjust Interval", "Seconds between adjustments.", 3, 1, 15, 1).unit("s"));
    public final BooleanSetting entityDistance = add(new BooleanSetting("Adjust Entity Distance", "Also scale entity render distance.", true));
    public final BooleanSetting particles = add(new BooleanSetting("Adjust Particles", "Lower particle density when below target.", true));

    private int ticks;
    private int originalDistance = -1;
    private double originalEntityScale = -1;

    public DynamicResolution() {
        super("Dynamic Resolution", "Adapts render distance and detail to hold a target FPS.", Category.RENDER);
        on(Events.TickPost.class, e -> tick());
    }

    @Override
    protected void onEnable() {
        originalDistance = mc.options.getViewDistance().getValue();
        originalEntityScale = mc.options.getEntityDistanceScaling().getValue();
    }

    @Override
    protected void onDisable() {
        if (originalDistance > 0) mc.options.getViewDistance().setValue(originalDistance);
        if (originalEntityScale > 0) mc.options.getEntityDistanceScaling().setValue(originalEntityScale);
    }

    private void tick() {
        if (++ticks < interval.get() * 20) return;
        ticks = 0;
        int fps = mc.getCurrentFps();
        int dist = mc.options.getViewDistance().getValue();
        double target = targetFps.get();
        if (fps < target * 0.9 && dist > distance.low()) dist--;
        else if (fps > target * 1.25 && dist < distance.high()) dist++;
        dist = (int) Math.max(distance.low(), Math.min(distance.high(), dist));
        if (dist != mc.options.getViewDistance().getValue()) mc.options.getViewDistance().setValue(dist);
        double ratio = Math.max(0.5, Math.min(1.0, fps / target));
        if (entityDistance.get()) mc.options.getEntityDistanceScaling().setValue(Math.max(0.5, Math.min(1.0, ratio)));
        CustomParticleQuality q = CustomParticleQuality.INSTANCE;
        if (particles.get() && q != null && q.isEnabled()) q.density.set(Math.round(ratio * 100 / 5.0) * 5.0);
    }

    @Override
    public String getInfo() { return mc.options == null ? null : mc.options.getViewDistance().getValue() + "ch"; }
}
