package voidclient.modules.world;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.EntityGroups;

import java.util.Set;

/** Live terrain minimap rendered into a dynamic texture from loaded chunks. */
public class Minimap extends HudModule {
    private static final int RES = 128;
    private static final Identifier TEXTURE_ID = Identifier.of("voidclient", "dynamic/minimap");

    public final ModeSetting shape = add(new ModeSetting("Shape", "Map shape.", "Circle", "Circle", "Square"));
    public final NumberSetting mapSize = add(new NumberSetting("Size", "Map size on screen.", 96, 48, 220, 2).unit("px"));
    public final NumberSetting zoom = add(new NumberSetting("Zoom", "Blocks per map pixel.", 1, 0.25, 4, 0.25));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate With Player", "Your facing is always up.", true));
    public final MultiSelectSetting entities = add(new MultiSelectSetting("Entities", "Entity groups drawn as dots.", EntityGroups.ALL,
            Set.of(EntityGroups.PLAYERS, EntityGroups.HOSTILE, EntityGroups.ITEMS)));
    public final BooleanSetting waypoints = add(new BooleanSetting("Waypoints", "Show waypoint markers.", true));
    public final BooleanSetting coords = add(new BooleanSetting("Coordinates", "Coordinates under the map.", true));
    public final BooleanSetting north = add(new BooleanSetting("North Marker", "N marker on the rim.", true));
    public final BooleanSetting caveMode = add(new BooleanSetting("Cave Mode", "Show a slice under you when underground.", true));
    public final NumberSetting updateRate = add(new NumberSetting("Update Rate", "Map rows redrawn per tick.", 32, 8, 128, 8));
    public final ColorSetting rim = add(new ColorSetting("Rim Color", "Map border colour.", 0xFFB026FF).themed());

    private NativeImageBackedTexture texture;
    private int row;
    private final BlockPos.Mutable tmp = new BlockPos.Mutable();

    public Minimap() {
        super("Minimap", "Rotating terrain minimap with entities and waypoints.", Category.WORLD, 0.865, 0.005);
        alsoListIn(Category.VISUALS);
        on(Events.TickPost.class, e -> update());
    }

    @Override
    protected void onDisable() {
        if (texture != null) {
            mc.getTextureManager().destroyTexture(TEXTURE_ID);
            texture = null;
        }
    }

    private void ensureTexture() {
        if (texture != null) return;
        texture = new NativeImageBackedTexture(() -> "Void minimap", RES, RES, false);
        mc.getTextureManager().registerTexture(TEXTURE_ID, texture);
    }

    private boolean underground() {
        if (!caveMode.get() || mc.world == null || mc.player == null) return false;
        BlockPos p = mc.player.getBlockPos();
        return !mc.world.isSkyVisible(p) && MapColors.surfaceY(mc.world, p.getX(), p.getZ()) > p.getY() + 3;
    }

    private void update() {
        if (mc.world == null || mc.player == null) return;
        ensureTexture();
        NativeImage img = texture.getImage();
        if (img == null) return;
        double px = mc.player.getX(), pz = mc.player.getZ();
        double scale = zoom.get();
        double yaw = Math.toRadians(mc.player.getYaw() + 180);
        double cos = rotate.get() ? Math.cos(yaw) : 1, sin = rotate.get() ? Math.sin(yaw) : 0;
        int caveY = underground() ? mc.player.getBlockPos().getY() + 1 : Integer.MIN_VALUE;
        boolean circle = shape.is("Circle");
        int rows = updateRate.getInt();
        for (int r = 0; r < rows; r++) {
            int y = row;
            row = (row + 1) % RES;
            for (int x = 0; x < RES; x++) {
                double dx = (x - RES / 2.0) * scale, dz = (y - RES / 2.0) * scale;
                if (circle && dx * dx + dz * dz > (RES / 2.0 * scale) * (RES / 2.0 * scale)) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                double wx = px + dx * cos - dz * sin, wz = pz + dx * sin + dz * cos;
                int c = MapColors.color(mc.world, (int) Math.floor(wx), (int) Math.floor(wz), caveY, tmp);
                img.setColorArgb(x, y, c == 0 ? 0xFF0A0A12 : c);
            }
        }
        texture.upload();
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float s = mapSize.getFloat();
        float extra = coords.get() ? 11 : 0;
        size(s, s + extra);
        boolean circle = shape.is("Circle");
        float r = s / 2f;
        if (circle) {
            Render2D.glow(ctx, 0, 0, s, s, r, 0.6f, rim.getColor());
            Render2D.circle(ctx, r, r, r, 0xC0000000);
        } else {
            panel(ctx, 0, 0, s, s);
        }
        if (texture != null) Render2D.texture(ctx, TEXTURE_ID, 0, 0, s, s, 0xFFFFFFFF);
        if (circle) Render2D.ring(ctx, r, r, r, 1.5f, rim.getColor());
        else Render2D.outline(ctx, 0, 0, s, s, 1, rim.getColor());
        if (mc.player == null) return;
        double scale = zoom.get() * (128.0 / s);
        double yaw = Math.toRadians(mc.player.getYaw() + 180);
        double cos = rotate.get() ? Math.cos(yaw) : 1, sin = rotate.get() ? Math.sin(yaw) : 0;
        if (mc.world != null) {
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !entities.isSelected(EntityGroups.of(e))) continue;
                double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
                double mx = (dx * cos + dz * sin) / scale, mz = (-dx * sin + dz * cos) / scale;
                if (circle ? mx * mx + mz * mz > (r - 3) * (r - 3) : Math.abs(mx) > r - 3 || Math.abs(mz) > r - 3) continue;
                int c = switch (EntityGroups.of(e)) {
                    case EntityGroups.PLAYERS -> 0xFFFFFFFF;
                    case EntityGroups.HOSTILE -> 0xFFFF3C3C;
                    case EntityGroups.ITEMS -> 0xFFFFD23C;
                    case EntityGroups.PASSIVE -> 0xFF3CFF8C;
                    default -> 0xFF9AA0FF;
                };
                Render2D.circle(ctx, (float) (r + mx), (float) (r + mz), 1.5f, c);
            }
        }
        if (waypoints.get()) {
            for (Waypoint w : WaypointStore.inCurrentDimension()) {
                if (!w.visible) continue;
                double dx = w.x + 0.5 - mc.player.getX(), dz = w.z + 0.5 - mc.player.getZ();
                double mx = (dx * cos + dz * sin) / scale, mz = (-dx * sin + dz * cos) / scale;
                double len = Math.sqrt(mx * mx + mz * mz);
                if (len > r - 4) {
                    mx = mx / len * (r - 4);
                    mz = mz / len * (r - 4);
                }
                Render2D.circle(ctx, (float) (r + mx), (float) (r + mz), 2.2f, w.color | 0xFF000000);
            }
        }
        if (north.get()) {
            double nx = rotate.get() ? -sin : 0, nz = rotate.get() ? -cos : -1;
            float tx = (float) (r + nx * (r - 6)), tz = (float) (r + nz * (r - 6));
            Render2D.circle(ctx, tx, tz, 4, 0xC0000000);
            FontRenderer.draw(ctx, "N", tx - 2.5f, tz - 3.5f, 0xFFFF5566, FontRenderer.Family.INTER_BOLD, false);
        }
        float ang = rotate.get() ? 0 : (float) Math.toRadians(mc.player.getYaw() + 180);
        float ax = (float) Math.sin(ang), az = (float) -Math.cos(ang);
        float bx = -az, bz = ax;
        Render2D.triangle(ctx, r + ax * 4.5f, r + az * 4.5f, r - ax * 3 + bx * 3, r - az * 3 + bz * 3, r - ax * 3 - bx * 3, r - az * 3 - bz * 3, 0xFFFFFFFF);
        if (coords.get()) {
            BlockPos p = mc.player.getBlockPos();
            String c = p.getX() + ", " + p.getY() + ", " + p.getZ();
            text(ctx, c, r - textWidth(c) / 2f, s + 2, textColor.getColor());
        }
    }

}
