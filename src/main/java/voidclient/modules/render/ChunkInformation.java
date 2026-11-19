package voidclient.modules.render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.utilities.ChunkTracker;

import java.util.List;

/** Current chunk, region, position inside the chunk and loaded-chunk statistics. */
public class ChunkInformation extends TextHudModule {
    public final BooleanSetting region = add(new BooleanSetting("Region", "Region file coordinates.", true));
    public final BooleanSetting local = add(new BooleanSetting("Local Position", "Block position inside the chunk.", true));
    public final BooleanSetting loaded = add(new BooleanSetting("Loaded Chunks", "Chunks the server has sent.", true));
    public final BooleanSetting section = add(new BooleanSetting("Section", "Vertical chunk section index.", false));
    public final BooleanSetting renderInfo = add(new BooleanSetting("Render Stats", "Chunk render statistics from the world renderer.", false));

    public ChunkInformation() {
        super("Chunk Information", "Chunk, region and loaded-chunk details.", Category.RENDER, 0.005, 0.40);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("Chunk", "12, -7"));
            return;
        }
        BlockPos p = mc.player.getBlockPos();
        ChunkPos c = new ChunkPos(p);
        out.add(new Line("Chunk", c.x + ", " + c.z));
        if (region.get()) out.add(new Line("Region", "r." + (c.x >> 5) + "." + (c.z >> 5) + ".mca"));
        if (local.get()) out.add(new Line("In Chunk", (p.getX() & 15) + ", " + (p.getY()) + ", " + (p.getZ() & 15)));
        if (section.get()) out.add(new Line("Section", String.valueOf(p.getY() >> 4)));
        if (loaded.get()) out.add(new Line("Loaded", String.valueOf(ChunkTracker.count())));
        if (renderInfo.get() && mc.worldRenderer != null) out.add(new Line("Render", String.valueOf(mc.worldRenderer.getChunksDebugString())));
    }
}
