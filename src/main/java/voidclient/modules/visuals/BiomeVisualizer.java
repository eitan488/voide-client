package voidclient.modules.visuals;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/** Ground tiles coloured per biome around you, with labels where biomes change. */
public class BiomeVisualizer extends Module {
    private record Tile(double x, double z, double y, int color, String label) {}

    public final NumberSetting radius = add(new NumberSetting("Radius", "Tiles drawn around you (blocks).", 32, 8, 96, 4).unit("m"));
    public final NumberSetting tile = add(new NumberSetting("Tile Size", "Tile size in blocks.", 4, 1, 8, 1));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Tile opacity.", 0.25, 0.05, 0.8, 0.05));
    public final BooleanSetting labels = add(new BooleanSetting("Labels", "Biome names at borders.", true));

    private final List<Tile> tiles = new ArrayList<>();
    private int ticks;

    public BiomeVisualizer() {
        super("Biome Visualizer", "Colour-coded biome map projected onto the ground.", Category.VISUALS);
        alsoListIn(Category.WORLD);
        on(Events.TickPost.class, e -> {
            if (++ticks % 20 == 0) rebuild();
        });
        on(Events.WorldRender.class, e -> {
            double s = tile.get();
            for (Tile t : tiles) {
                RenderManager.flatQuad(t.x() + 0.05, t.z() + 0.05, t.x() + s - 0.05, t.z() + s - 0.05, t.y(), ColorUtil.withAlpha(t.color(), Math.round(opacity.getFloat() * 255)), false);
                if (t.label() != null) RenderManager.text(Text.literal(t.label()), new Vec3d(t.x() + s / 2, t.y() + 0.6, t.z() + s / 2), 0.2f, t.color() | 0xFF000000, 0x80000000, false, true);
            }
        });
    }

    private static int colorOf(String key) { return ColorUtil.hsb((key.hashCode() & 0xFFFF) / 65535f, 0.65f, 1f); }

    private void rebuild() {
        tiles.clear();
        if (mc.world == null || mc.player == null) return;
        int s = tile.getInt(), r = radius.getInt();
        BlockPos p = mc.player.getBlockPos();
        int bx = Math.floorDiv(p.getX(), s) * s, bz = Math.floorDiv(p.getZ(), s) * s;
        for (int x = -r; x <= r; x += s) for (int z = -r; z <= r; z += s) {
            int wx = bx + x, wz = bz + z;
            int y = voidclient.modules.world.MapColors.surfaceY(mc.world, wx, wz);
            if (y == Integer.MIN_VALUE) continue;
            RegistryEntry<Biome> b = mc.world.getBiome(new BlockPos(wx, y, wz));
            String key = b.getKey().map(k -> k.getValue().getPath()).orElse("?");
            String east = mc.world.getBiome(new BlockPos(wx + s, y, wz)).getKey().map(k -> k.getValue().getPath()).orElse("?");
            String label = labels.get() && !east.equals(key) ? key.replace('_', ' ') : null;
            tiles.add(new Tile(wx, wz, y + 0.02, colorOf(key), label));
        }
    }

    @Override
    protected void onDisable() { tiles.clear(); }
}
