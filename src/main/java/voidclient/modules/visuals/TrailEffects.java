package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayDeque;
import java.util.Iterator;

/** A glowing trail behind you: line ribbon or particles. */
public class TrailEffects extends ParticleModule {
    public final ModeSetting style = add(new ModeSetting("Style", "Trail style.", "Ribbon", "Ribbon", "Line", "Particles"));
    public final NumberSetting length = add(new NumberSetting("Length", "Trail length in ticks.", 30, 5, 200, 1));
    public final NumberSetting width = add(new NumberSetting("Width", "Ribbon / line width.", 0.3, 0.05, 1.5, 0.05));
    public final BooleanSetting onlyMoving = add(new BooleanSetting("Only While Moving", "Stop recording while standing still.", true));
    public final BooleanSetting firstPerson = add(new BooleanSetting("Show In First Person", "Render while in first person.", true));

    private final ArrayDeque<Vec3d> points = new ArrayDeque<>();

    public TrailEffects() {
        super("Trail Effects", "Neon trail that follows your movement.");
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            Vec3d p = mc.player.getEntityPos().add(0, 0.1, 0);
            boolean moving = mc.player.getVelocity().lengthSquared() > 1e-4;
            if (!onlyMoving.get() || moving) {
                points.addFirst(p);
                if (style.is("Particles") && roll()) emit(p, 0, 0, 0, 0.3f);
            }
            while (points.size() > length.getInt()) points.removeLast();
        });
        on(Events.WorldRender.class, e -> {
            if (style.is("Particles") || points.size() < 2) return;
            if (!firstPerson.get() && mc.options.getPerspective().isFirstPerson()) return;
            Iterator<Vec3d> it = points.iterator();
            Vec3d prev = it.next();
            int i = 0, n = points.size();
            while (it.hasNext()) {
                Vec3d cur = it.next();
                float f = 1f - i / (float) n;
                int c = ColorUtil.multiplyAlpha(color.getColor(i / (float) n * 0.5f), f * (1f - transparency.getFloat()));
                if (style.is("Line")) RenderManager.line(prev, cur, c, width.getFloat() * 10, false);
                else {
                    double hw = width.get() / 2 * f;
                    RenderManager.boxFilled(new net.minecraft.util.math.Box(Math.min(prev.x, cur.x) - hw, prev.y, Math.min(prev.z, cur.z) - hw,
                            Math.max(prev.x, cur.x) + hw, prev.y + 0.02, Math.max(prev.z, cur.z) + hw), c, false);
                }
                prev = cur;
                i++;
            }
        });
    }

    @Override
    protected void onDisable() { points.clear(); }
}
