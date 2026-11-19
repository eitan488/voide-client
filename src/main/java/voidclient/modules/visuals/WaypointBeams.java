package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.gui.themes.ThemeManager;
import voidclient.modules.world.Waypoint;
import voidclient.modules.world.WaypointStore;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

/** Beacon-style light beams at every visible waypoint. */
public class WaypointBeams extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", "Beam height.", 128, 8, 320, 8).unit("m"));
    public final NumberSetting width = add(new NumberSetting("Width", "Beam half-width.", 0.25, 0.05, 1.5, 0.05));
    public final ModeSetting colorMode = add(new ModeSetting("Color", "Beam colour source.", "Waypoint", "Waypoint", "Theme"));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Beams visible through terrain.", true));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Beams further than this are hidden.", 512, 32, 4096, 32).unit("m"));

    public WaypointBeams() {
        super("Waypoint Beams", "Light beams marking your waypoints.", Category.VISUALS);
        alsoListIn(Category.WORLD);
        on(Events.WorldRender.class, e -> {
            if (mc.player == null) return;
            for (Waypoint w : WaypointStore.inCurrentDimension()) {
                if (!w.visible) continue;
                Vec3d base = new Vec3d(w.x + 0.5, w.y, w.z + 0.5);
                double d = base.distanceTo(mc.player.getEntityPos());
                if (d > maxDistance.get()) continue;
                Vec3d cam = RenderManager.camera();
                if (d > 160) base = cam.add(base.subtract(cam).multiply(160 / d));
                int c = colorMode.is("Theme") ? ThemeManager.accent(0) : w.color | 0xFF000000;
                double pulse = 1 + 0.15 * Math.sin(System.currentTimeMillis() / 400.0);
                RenderManager.beam(base, height.get(), width.get() * pulse, ColorUtil.withAlpha(c, 150), ColorUtil.withAlpha(c, 0), throughWalls.get());
                RenderManager.beam(base, height.get() * 0.6, width.get() * 0.35, ColorUtil.withAlpha(0xFFFFFFFF, 120), ColorUtil.withAlpha(c, 0), throughWalls.get());
            }
        });
    }
}
