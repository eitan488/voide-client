package voidclient.modules.world;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import voidclient.VoidClient;
import voidclient.events.Events;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explored-terrain cache for the World Map: 512x512-block region textures filled from chunks
 * the server sends, persisted as PNGs under config/voidclient/maps/<world>/<dimension>/.
 */
public final class MapCache {
    public static final int REGION = 512;
    private static final int MAX_LOADED = 48;

    public static final class Region {
        final int rx, rz;
        final NativeImageBackedTexture texture;
        final Identifier id;
        boolean dirty, uploaded;

        Region(int rx, int rz, NativeImage image) {
            this.rx = rx;
            this.rz = rz;
            this.texture = new NativeImageBackedTexture(() -> "Void map region", image);
            this.id = Identifier.of("voidclient", "dynamic/map/" + Integer.toString(rx).replace('-', 'm') + "_" + Integer.toString(rz).replace('-', 'm'));
        }

        public Identifier id() { return id; }
        public int rx() { return rx; }
        public int rz() { return rz; }
    }

    private static final Map<Long, Region> REGIONS = new LinkedHashMap<>(16, 0.75f, true);
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static String worldKey, dimension;
    private static int ticks;
    private static boolean enabled = true;

    private MapCache() {}

    public static void setEnabled(boolean e) { enabled = e; }

    public static void init() {
        var bus = VoidClient.get().events();
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (enabled) PENDING.add(chunk.getPos().toLong());
        });
        bus.subscribe(Events.WorldJoin.class, e -> worldKey = e.worldKey());
        bus.subscribe(Events.WorldLeave.class, e -> {
            saveAll();
            clear();
            worldKey = null;
        });
        bus.subscribe(Events.TickPost.class, e -> tick());
    }

    private static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || worldKey == null || !enabled) return;
        String dim = mc.world.getRegistryKey().getValue().toString().replace(':', '_');
        if (!dim.equals(dimension)) {
            saveAll();
            clear();
            dimension = dim;
        }
        for (int i = 0; i < 6 && !PENDING.isEmpty(); i++) {
            long key = PENDING.poll();
            WorldChunk c = voidclient.utilities.ChunkTracker.get((int) key, (int) (key >> 32));
            if (c != null) drawChunk(c);
        }
        if (++ticks % 100 == 0 && mc.player != null) {
            int cx = mc.player.getChunkPos().x, cz = mc.player.getChunkPos().z;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                WorldChunk c = voidclient.utilities.ChunkTracker.get(cx + x, cz + z);
                if (c != null) drawChunk(c);
            }
        }
        if (ticks % 6000 == 0) saveAll();
        for (Region r : REGIONS.values()) {
            if (r.dirty) {
                r.texture.upload();
                r.dirty = false;
            }
        }
    }

    private static Path dir() {
        return VoidClient.get().config().dir().resolve("maps").resolve(worldKey).resolve(dimension == null ? "unknown" : dimension);
    }

    public static Region region(int rx, int rz, boolean create) {
        long key = (((long) rz) << 32) | (rx & 0xFFFFFFFFL);
        Region r = REGIONS.get(key);
        if (r != null || !create || worldKey == null) return r;
        NativeImage img = null;
        Path file = dir().resolve("r." + rx + "." + rz + ".png");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                img = NativeImage.read(in);
                if (img.getWidth() != REGION || img.getHeight() != REGION) {
                    img.close();
                    img = null;
                }
            } catch (IOException ex) {
                VoidClient.LOG.warn("Could not read map region {}", file, ex);
            }
        }
        if (img == null) img = new NativeImage(REGION, REGION, true);
        r = new Region(rx, rz, img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(r.id, r.texture);
        r.dirty = true;
        REGIONS.put(key, r);
        evict();
        return r;
    }

    private static void evict() {
        Iterator<Map.Entry<Long, Region>> it = REGIONS.entrySet().iterator();
        while (REGIONS.size() > MAX_LOADED && it.hasNext()) {
            Region r = it.next().getValue();
            save(r);
            MinecraftClient.getInstance().getTextureManager().destroyTexture(r.id);
            it.remove();
        }
    }

    public static void drawChunk(WorldChunk chunk) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        int bx = chunk.getPos().getStartX(), bz = chunk.getPos().getStartZ();
        Region r = region(Math.floorDiv(bx, REGION), Math.floorDiv(bz, REGION), true);
        if (r == null) return;
        NativeImage img = r.texture.getImage();
        if (img == null) return;
        BlockPos.Mutable tmp = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int c = MapColors.color(mc.world, bx + x, bz + z, Integer.MIN_VALUE, tmp);
            if (c != 0) img.setColorArgb(Math.floorMod(bx + x, REGION), Math.floorMod(bz + z, REGION), c);
        }
        r.dirty = true;
        r.uploaded = true;
    }

    public static Map<Long, Region> loaded() { return REGIONS; }

    private static void save(Region r) {
        if (!r.uploaded || worldKey == null || r.texture.getImage() == null) return;
        try {
            Files.createDirectories(dir());
            r.texture.getImage().writeTo(dir().resolve("r." + r.rx + "." + r.rz + ".png"));
        } catch (IOException e) {
            VoidClient.LOG.warn("Could not save map region", e);
        }
    }

    public static void saveAll() {
        for (Region r : REGIONS.values()) save(r);
    }

    private static void clear() {
        for (Region r : REGIONS.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(r.id);
        REGIONS.clear();
        PENDING.clear();
    }

}
