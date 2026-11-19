package voidclient.modules.basehunting;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import voidclient.events.Events;
import voidclient.modules.world.Waypoint;
import voidclient.modules.world.WaypointStore;
import voidclient.render.RenderManager;

/** Through-wall boxes and distance labels for your waypoints. */
public class WaypointEsp extends EspModule {
    public WaypointEsp() {
        super("Waypoint ESP", "Boxes and labels for waypoints, visible through walls.");
        renderDistance.defaultTo(512.0);
        on(Events.WorldRender.class, e -> {
            if (mc.player == null) return;
            for (Waypoint w : WaypointStore.inCurrentDimension()) {
                if (!w.visible) continue;
                Vec3d c = new Vec3d(w.x + 0.5, w.y + 0.5, w.z + 0.5);
                if (!inRange(c)) continue;
                Vec3d cam = RenderManager.camera();
                double d = c.distanceTo(cam);
                Vec3d shown = d > 96 ? cam.add(c.subtract(cam).multiply(96 / d)) : c;
                double s = d > 96 ? 0.5 * 96 / d : 0.5;
                drawBox(new Box(shown.x - s, shown.y - s, shown.z - s, shown.x + s, shown.y + s, shown.z + s), w.color | 0xFF000000);
                drawLabel(w.name, shown.add(0, s + 0.4, 0), w.color | 0xFF000000);
            }
        });
    }
}
