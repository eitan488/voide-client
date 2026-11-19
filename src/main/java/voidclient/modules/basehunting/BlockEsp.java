package voidclient.modules.basehunting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.StringSetting;
import voidclient.events.Events;
import voidclient.utilities.ChunkTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Highlights chosen block types inside loaded chunks (palette-filtered, budgeted scan). */
public class BlockEsp extends EspModule {
    public final StringSetting blocks = add(new StringSetting("Blocks", "Comma-separated block ids.", "spawner,end_portal_frame,beacon,respawn_anchor,budding_amethyst", 800));
    public final NumberSetting rescan = add(new NumberSetting("Rescan Interval", "Ticks between rescans.", 60, 10, 600, 10));

    private final List<BlockPos> found = new ArrayList<>();
    private final List<BlockPos> building = new ArrayList<>();
    private final List<WorldChunk> pending = new ArrayList<>();
    private Set<Block> wanted = new HashSet<>();
    private String key = "";
    private int ticks;

    public BlockEsp() {
        super("Block ESP", "Highlights the block types you list.");
        renderDistance.defaultTo(64.0);
        on(Events.TickPost.class, e -> tick());
        on(Events.WorldRender.class, e -> {
            for (BlockPos p : found) {
                Vec3d c = Vec3d.ofCenter(p);
                if (!inRange(c)) continue;
                drawBox(new Box(p), color.getColor());
                if (mc.world != null) drawLabel(mc.world.getBlockState(p).getBlock().getName().getString(), c.add(0, 0.8, 0), color.getColor());
            }
        });
    }

    private void resolve() {
        if (blocks.get().equals(key)) return;
        key = blocks.get();
        wanted = new HashSet<>();
        for (String s : key.split(",")) {
            if (s.isBlank()) continue;
            Identifier id = Identifier.tryParse(s.contains(":") ? s.trim() : "minecraft:" + s.trim());
            if (id != null && Registries.BLOCK.containsId(id)) wanted.add(Registries.BLOCK.get(id));
        }
    }

    private void tick() {
        if (mc.player == null) return;
        resolve();
        if (pending.isEmpty() && ticks++ % rescan.getInt() == 0) {
            ChunkPos pc = mc.player.getChunkPos();
            pending.addAll(ChunkTracker.around(pc.x, pc.z, (int) Math.ceil(renderDistance.get() / 16.0)));
            building.clear();
        }
        long start = System.nanoTime();
        while (!pending.isEmpty() && System.nanoTime() - start < 2_000_000) {
            scan(pending.remove(pending.size() - 1));
            if (pending.isEmpty()) {
                found.clear();
                found.addAll(building);
            }
        }
    }

    private void scan(WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        int bottom = chunk.getBottomSectionCoord();
        ChunkPos cp = chunk.getPos();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection sec = sections[i];
            if (sec == null || sec.isEmpty() || !sec.hasAny(st -> wanted.contains(st.getBlock()))) continue;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState st = sec.getBlockState(x, y, z);
                if (wanted.contains(st.getBlock()) && building.size() < maxCount.getInt()) {
                    building.add(new BlockPos(cp.getStartX() + x, ((bottom + i) << 4) + y, cp.getStartZ() + z));
                }
            }
        }
    }

    @Override
    public String getInfo() { return String.valueOf(found.size()); }
}
