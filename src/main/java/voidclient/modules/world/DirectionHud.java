package voidclient.modules.world;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Scrolling compass strip with degrees and waypoint markers. */
public class DirectionHud extends HudModule {
    public final NumberSetting width = add(new NumberSetting("Width", "Compass width.", 180, 80, 400, 5).unit("px"));
    public final NumberSetting fov = add(new NumberSetting("Span", "Degrees visible across the strip.", 120, 60, 240, 5).unit("°"));
    public final BooleanSetting degrees = add(new BooleanSetting("Degrees", "Numeric heading below the strip.", true));
    public final BooleanSetting waypoints = add(new BooleanSetting("Waypoints", "Markers for waypoints.", true));

    public DirectionHud() {
        super("Direction HUD", "Compass strip with heading and waypoint markers.", Category.WORLD, 0.37, 0.005);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float w = width.getFloat(), h = degrees.get() ? 24 : 14;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        float yaw = mc.player == null ? 0 : MathHelper.wrapDegrees(mc.player.getYaw(delta));
        float span = fov.getFloat();
        Render2D.scissor(ctx, 2, 0, w - 4, h);
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        for (int deg = -180; deg < 540; deg += 15) {
            float rel = MathHelper.wrapDegrees(deg - yaw);
            if (Math.abs(rel) > span / 2) continue;
            float x = w / 2f + rel / span * w;
            boolean major = deg % 45 == 0;
            Render2D.rect(ctx, x - 0.25f, 1, 0.5f, major ? 4 : 2, ColorUtil.withAlpha(textColor.getColor(), major ? 220 : 100));
            if (major) {
                String n = names[Math.floorMod(deg / 45, 8)];
                text(ctx, n, x - textWidth(n) / 2f, 5, n.equals("N") ? 0xFFFF5566 : textColor.getColor());
            }
        }
        if (waypoints.get() && mc.player != null) {
            for (Waypoint wp : WaypointStore.inCurrentDimension()) {
                if (!wp.visible) continue;
                double dx = wp.x + 0.5 - mc.player.getX(), dz = wp.z + 0.5 - mc.player.getZ();
                float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float rel = MathHelper.wrapDegrees(target - yaw);
                if (Math.abs(rel) > span / 2) continue;
                float x = w / 2f + rel / span * w;
                Render2D.triangle(ctx, x - 3, 0, x + 3, 0, x, 4, wp.color | 0xFF000000);
            }
        }
        Render2D.endScissor(ctx);
        Render2D.rect(ctx, w / 2f - 0.5f, 0, 1, 14, accent());
        if (degrees.get()) {
            String d = String.format(Locale.ROOT, "%.0f°", (yaw + 360) % 360);
            text(ctx, d, w / 2f - textWidth(d) / 2f, 14, accent());
        }
    }
}
