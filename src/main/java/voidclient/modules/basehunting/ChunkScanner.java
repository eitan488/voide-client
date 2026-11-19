package voidclient.modules.basehunting;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import voidclient.utilities.ChunkTracker;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incremental, time-budgeted scanner over chunks the server has sent. Sections whose palette
 * contains none of the tracked blocks are skipped without iterating their 4096 blocks.
 */
public final class ChunkScanner {
    private final Map<Long, ChunkStats> results = new HashMap<>();
    private final List<Long> queue = new ArrayList<>();

    public Map<Long, ChunkStats> results() { return results; }

    public ChunkStats get(int cx, int cz) { return results.get(ChunkPos.toLong(cx, cz)); }

    public void clear() {
        results.clear();
        queue.clear();
    }

    /** Rebuilds the queue: nearest chunks first, only loaded ones. */
    public void schedule(int cx, int cz, int radius) {
        queue.clear();
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                if (ChunkTracker.isLoaded(cx + x, cz + z)) queue.add(ChunkPos.toLong(cx + x, cz + z));
            }
        }
        results.keySet().removeIf(k -> Math.max(Math.abs(ChunkPos.getPackedX(k) - cx), Math.abs(ChunkPos.getPackedZ(k) - cz)) > radius + 1);
    }

    /** Scans queued chunks until the nanosecond budget is used. Returns chunks completed. */
    public List<ChunkStats> tick(Set<Component> tracked, String dimension, long budgetNanos) {
        List<ChunkStats> done = new ArrayList<>();
        long start = System.nanoTime();
        while (!queue.isEmpty() && System.nanoTime() - start < budgetNanos) {
            long key = queue.remove(0);
            WorldChunk chunk = ChunkTracker.get(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
            if (chunk == null) continue;
            ChunkStats s = scan(chunk, tracked, dimension);
            results.put(key, s);
            done.add(s);
        }
        return done;
    }

    public boolean idle() { return queue.isEmpty(); }

    public static ChunkStats scan(WorldChunk chunk, Set<Component> tracked, String dimension) {
        ChunkPos cp = chunk.getPos();
        ChunkStats stats = new ChunkStats(cp.x, cp.z, dimension);
        Set<Component> blockTracked = EnumSet.noneOf(Component.class);
        blockTracked.addAll(tracked);
        boolean collection = blockTracked.remove(Component.COLLECTION);
        if (collection) blockTracked.add(Component.HOPPER);
        List<BlockPos> hoppers = new ArrayList<>();
        ChunkSection[] sections = chunk.getSectionArray();
        int bottom = chunk.getBottomSectionCoord();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection sec = sections[i];
            if (sec == null || sec.isEmpty()) continue;
            if (!sec.hasAny(st -> matchesAny(st, blockTracked))) continue;
            int baseY = (bottom + i) << 4;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState st = sec.getBlockState(x, y, z);
                Component c = Component.match(st);
                if (c == null || !blockTracked.contains(c)) continue;
                stats.counts.merge(c, 1, Integer::sum);
                int wy = baseY + y;
                if (wy > stats.topY) stats.topY = wy;
                if (c == Component.HOPPER && collection) hoppers.add(new BlockPos(cp.getStartX() + x, wy, cp.getStartZ() + z));
            }
        }
        if (collection) {
            int links = 0;
            for (BlockPos h : hoppers) {
                BlockState hs = chunk.getBlockState(h);
                if (!hs.isOf(Blocks.HOPPER)) continue;
                Direction facing = hs.get(HopperBlock.FACING);
                BlockEntity target = chunk.getWorld().getBlockEntity(pos.set(h).move(facing));
                if (target instanceof LootableContainerBlockEntity) links++;
            }
            if (links > 0) stats.counts.put(Component.COLLECTION, links);
            if (!tracked.contains(Component.HOPPER)) stats.counts.remove(Component.HOPPER);
        }
        return stats;
    }

    private static boolean matchesAny(BlockState st, Set<Component> tracked) {
        Component c = Component.match(st);
        return c != null && tracked.contains(c);
    }
}
