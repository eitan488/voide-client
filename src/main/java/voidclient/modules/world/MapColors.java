package voidclient.modules.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

/** Top-down terrain colours from client chunk data (vanilla map colour palette). */
public final class MapColors {
    private MapColors() {}

    /** Height of the surface column, or Integer.MIN_VALUE if the chunk is not loaded. */
    public static int surfaceY(ClientWorld world, int x, int z) {
        WorldChunk c = voidclient.utilities.ChunkTracker.get(x >> 4, z >> 4);
        if (c == null) return Integer.MIN_VALUE;
        return c.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);
    }

    /**
     * ARGB colour of a column. {@code caveY} &gt; MIN uses a cave slice below that height.
     * {@code northY} is the neighbour surface used for relief shading.
     */
    public static int color(ClientWorld world, int x, int z, int caveY, BlockPos.Mutable tmp) {
        WorldChunk chunk = voidclient.utilities.ChunkTracker.get(x >> 4, z >> 4);
        if (chunk == null) return 0;
        int y;
        if (caveY != Integer.MIN_VALUE) {
            y = Integer.MIN_VALUE;
            for (int yy = caveY; yy > caveY - 24 && yy > world.getBottomY(); yy--) {
                tmp.set(x, yy, z);
                if (!chunk.getBlockState(tmp).isAir() && chunk.getBlockState(tmp.move(0, 1, 0)).isAir()) {
                    y = yy;
                    break;
                }
            }
            if (y == Integer.MIN_VALUE) return 0xFF050508;
        } else {
            y = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15) - 1;
        }
        if (y < world.getBottomY()) return 0;
        tmp.set(x, y, z);
        BlockState state = chunk.getBlockState(tmp);
        FluidState fluid = state.getFluidState();
        int depth = 0;
        if (fluid.isIn(FluidTags.WATER)) {
            while (depth < 12) {
                tmp.move(0, -1, 0);
                if (!chunk.getBlockState(tmp).getFluidState().isIn(FluidTags.WATER)) break;
                depth++;
            }
        }
        MapColor mc = state.getMapColor(world, tmp.set(x, y, z));
        if (mc == MapColor.CLEAR) return 0;
        int northY = caveY != Integer.MIN_VALUE ? y : chunkHeight(world, x, z - 1, y);
        MapColor.Brightness b;
        if (depth > 0) b = depth > 6 ? MapColor.Brightness.LOW : depth > 2 ? MapColor.Brightness.NORMAL : MapColor.Brightness.HIGH;
        else b = y > northY ? MapColor.Brightness.HIGH : y < northY ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        return mc.getRenderColor(b);
    }

    private static int chunkHeight(ClientWorld world, int x, int z, int fallback) {
        WorldChunk c = voidclient.utilities.ChunkTracker.get(x >> 4, z >> 4);
        if (c == null) return fallback;
        return c.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15) - 1;
    }
}
