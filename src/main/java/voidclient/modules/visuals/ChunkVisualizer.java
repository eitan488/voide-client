package voidclient.modules.visuals;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

/** Chunk borders: current chunk walls, neighbouring corners and section grid lines. */
public class ChunkVisualizer extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", "Chunks around you with corner lines.", 2, 0, 6, 1));
    public final ColorSetting currentColor = add(new ColorSetting("Current Chunk", "Colour of your chunk's border.", 0xFF00F0FF).themed());
    public final ColorSetting otherColor = add(new ColorSetting("Other Chunks", "Corner line colour.", 0x80FFC93C));
    public final BooleanSetting sections = add(new BooleanSetting("Section Lines", "Horizontal lines every 16 blocks.", true));
    public final BooleanSetting grid = add(new BooleanSetting("Block Grid", "2-block grid on your chunk's walls.", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw through terrain.", false));

    public ChunkVisualizer() {
        super("Chunk Visualizer", "Chunk borders and section lines around you.", Category.VISUALS);
        alsoListIn(Category.BASE_HUNTING, Category.WORLD);
        on(Events.WorldRender.class, e -> {
            if (mc.player == null || mc.world == null) return;
            ChunkPos c = mc.player.getChunkPos();
            double bottom = mc.world.getBottomY(), top = mc.world.getTopYInclusive() + 1;
            boolean xr = throughWalls.get();
            int r = range.getInt();
            for (int dx = -r; dx <= r + 1; dx++) for (int dz = -r; dz <= r + 1; dz++) {
                double x = (c.x + dx) * 16.0, z = (c.z + dz) * 16.0;
                boolean own = (dx == 0 || dx == 1) && (dz == 0 || dz == 1);
                RenderManager.line(new Vec3d(x, bottom, z), new Vec3d(x, top, z), own ? currentColor.getColor() : otherColor.getColor(), own ? 2.5f : 1.5f, xr);
            }
            double x0 = c.x * 16.0, z0 = c.z * 16.0;
            int cc = ColorUtil.multiplyAlpha(currentColor.getColor(), 0.7f);
            if (sections.get()) {
                for (double y = Math.floor(bottom / 16) * 16; y <= top; y += 16) {
                    RenderManager.line(new Vec3d(x0, y, z0), new Vec3d(x0 + 16, y, z0), cc, 1.5f, xr);
                    RenderManager.line(new Vec3d(x0 + 16, y, z0), new Vec3d(x0 + 16, y, z0 + 16), cc, 1.5f, xr);
                    RenderManager.line(new Vec3d(x0 + 16, y, z0 + 16), new Vec3d(x0, y, z0 + 16), cc, 1.5f, xr);
                    RenderManager.line(new Vec3d(x0, y, z0 + 16), new Vec3d(x0, y, z0), cc, 1.5f, xr);
                }
            }
            if (grid.get()) {
                double py = Math.floor(mc.player.getY());
                int g = ColorUtil.multiplyAlpha(currentColor.getColor(), 0.35f);
                for (int i = 2; i < 16; i += 2) {
                    for (double[] w : new double[][]{{x0 + i, z0, x0 + i, z0}, {x0 + i, z0 + 16, x0 + i, z0 + 16}, {x0, z0 + i, x0, z0 + i}, {x0 + 16, z0 + i, x0 + 16, z0 + i}}) {
                        RenderManager.line(new Vec3d(w[0], py - 8, w[1]), new Vec3d(w[2], py + 8, w[3]), g, 1f, xr);
                    }
                }
            }
        });
    }
}
