package voidclient.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Set of chunks the server has sent to this client (the only chunk data we may inspect). */
public final class ChunkTracker {
    private static final Map<Long, WorldChunk> LOADED = new ConcurrentHashMap<>();

    private ChunkTracker() {}

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> LOADED.put(chunk.getPos().toLong(), chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> LOADED.remove(chunk.getPos().toLong()));
    }

    public static void clear() { LOADED.clear(); }

    public static int count() { return LOADED.size(); }

    public static WorldChunk get(int cx, int cz) { return LOADED.get(ChunkPos.toLong(cx, cz)); }

    public static boolean isLoaded(int cx, int cz) { return LOADED.containsKey(ChunkPos.toLong(cx, cz)); }

    /** Loaded chunks within a square chunk radius of (cx, cz). */
    public static List<WorldChunk> around(int cx, int cz, int radius) {
        List<WorldChunk> out = new ArrayList<>();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                WorldChunk c = LOADED.get(ChunkPos.toLong(x, z));
                if (c != null) out.add(c);
            }
        }
        return out;
    }

    public static java.util.Collection<WorldChunk> all() { return LOADED.values(); }
}
