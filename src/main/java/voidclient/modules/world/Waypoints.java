package voidclient.modules.world;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.keybinds.Keybind;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Saved locations with in-world labels, death points and a quick-add key. */
public class Waypoints extends Module {
    public static Waypoints INSTANCE;

    public final BooleanSetting labels = add(new BooleanSetting("Labels", "Draw waypoint names in the world.", true));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", "Append the distance to labels.", true));
    public final NumberSetting labelScale = add(new NumberSetting("Label Scale", "Label size.", 1, 0.3, 3, 0.05));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Hide waypoints further than this (0 = no limit).", 0, 0, 30000, 100).unit("m"));
    public final BooleanSetting deathPoints = add(new BooleanSetting("Death Waypoints", "Create a waypoint where you die.", true));
    public final KeybindSetting addKey = add(new KeybindSetting("Add Waypoint Key", "Create a waypoint at your position.", Keybind.key(org.lwjgl.glfw.GLFW.GLFW_KEY_B)));
    public final ColorSetting newColor = add(new ColorSetting("New Waypoint Color", "Colour for new waypoints.", 0xFF00F0FF, false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw labels through blocks.", true));

    private boolean wasDead;

    public Waypoints() {
        super("Waypoints", "Save locations, show them in the world, auto death points.", Category.WORLD);
        INSTANCE = this;
        WaypointStore.init();
        on(Events.Key.class, e -> {
            if (e.action == 1 && mc.currentScreen == null && mc.player != null && addKey.get().matchesKey(e.key)) {
                var p = mc.player.getBlockPos();
                Waypoint w = WaypointStore.add("Waypoint " + (WaypointStore.all().size() + 1), p.getX(), p.getY(), p.getZ(), newColor.getColor(), "Manual");
                if (w != null) Notifications.success("Waypoint added", w.name + " at " + p.getX() + " " + p.getY() + " " + p.getZ());
            }
        });
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0;
            if (dead && !wasDead) {
                voidclient.VoidClient.get().events().post(new Events.Death());
                if (deathPoints.get()) {
                    var p = mc.player.getBlockPos();
                    Waypoint w = WaypointStore.add("Death " + java.time.LocalTime.now().withNano(0), p.getX(), p.getY(), p.getZ(), 0xFFFF3C3C, "Death");
                    if (w != null) Notifications.warning("Death point saved", p.getX() + " " + p.getY() + " " + p.getZ());
                }
            }
            wasDead = dead;
        });
        on(Events.WorldRender.class, e -> {
            if (!labels.get() || mc.player == null) return;
            for (Waypoint w : WaypointStore.inCurrentDimension()) {
                if (!w.visible) continue;
                Vec3d pos = new Vec3d(w.x + 0.5, w.y + 1.5, w.z + 0.5);
                double d = pos.distanceTo(mc.player.getEyePos());
                if (maxDistance.get() > 0 && d > maxDistance.get()) continue;
                Vec3d cam = RenderManager.camera();
                Vec3d shown = d > 96 ? cam.add(pos.subtract(cam).normalize().multiply(96)) : pos;
                String label = w.name + (distance.get() ? String.format(Locale.ROOT, " [%.0fm]", d) : "");
                RenderManager.text(Text.literal(label), shown, labelScale.getFloat() * 0.25f, w.color | 0xFF000000, ColorUtil.withAlpha(0, 140), throughWalls.get(), true);
            }
        });
    }
}
