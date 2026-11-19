package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

/** Animated rings, disc or spiral at your feet. */
public class CosmeticAura extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Aura shape.", "Double Ring", "Ring", "Double Ring", "Disc", "Spiral"));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Aura radius.", 0.9, 0.3, 3, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", "Aura colour.", 0xFF00F0FF).themed());
    public final NumberSetting speed = add(new NumberSetting("Speed", "Animation speed.", 1, 0.1, 5, 0.1));
    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", "Breathing radius.", true));
    public final NumberSetting height = add(new NumberSetting("Height", "Vertical offset.", 0.05, 0, 2, 0.05));
    public final BooleanSetting firstPerson = add(new BooleanSetting("Show In First Person", "Render in first person.", true));

    public CosmeticAura() {
        super("Cosmetic Aura", "Animated aura around your feet.", Category.VISUALS);
        on(Events.WorldRender.class, e -> {
            if (mc.player == null || (!firstPerson.get() && mc.options.getPerspective().isFirstPerson())) return;
            Vec3d base = mc.player.getLerpedPos(e.tickDelta).add(0, height.get(), 0);
            double t = System.currentTimeMillis() / 1000.0 * speed.get();
            double r = radius.get() * (pulse.get() ? 0.9 + 0.1 * Math.sin(t * 2) : 1);
            int c = color.getColor();
            switch (mode.get()) {
                case "Ring" -> RenderManager.ring(base, r, c, 2.5f, 48, false);
                case "Disc" -> RenderManager.disc(base, r, ColorUtil.withAlpha(c, 110), ColorUtil.withAlpha(c, 0), 48, false);
                case "Spiral" -> {
                    Vec3d prev = null;
                    for (int i = 0; i <= 96; i++) {
                        double a = t * 2 + i * 0.25, h = i / 96.0 * mc.player.getHeight();
                        Vec3d p = base.add(Math.cos(a) * r, h, Math.sin(a) * r);
                        if (prev != null) RenderManager.line(prev, p, ColorUtil.multiplyAlpha(c, 1f - i / 96f), 2f, false);
                        prev = p;
                    }
                }
                default -> {
                    RenderManager.ring(base, r, c, 2.5f, 48, false);
                    RenderManager.ring(base.add(0, 0.25 + 0.15 * Math.sin(t * 3), 0), r * 0.75, ColorUtil.multiplyAlpha(c, 0.6f), 1.8f, 48, false);
                }
            }
        });
    }
}
