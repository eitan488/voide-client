package voidclient.modules.anchor;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import voidclient.core.Category;
import voidclient.events.Events;
import voidclient.modules.training.TrainerBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anchor practice: tracks your place -> charge -> detonate cycles from block updates the
 * server sends, plus dummy damage. Note anchors only explode outside the Nether.
 */
public class AnchorTrainer extends TrainerBase {
    private final Map<BlockPos, Long> placed = new HashMap<>();
    private final Map<BlockPos, Integer> charges = new HashMap<>();
    private int glowstoneUsed;

    public AnchorTrainer() {
        super("Anchor Practice Trainer", "Practice anchor place-charge-detonate cycles on dummies.", Category.ANCHOR,
                "Static Dummy", "Moving Dummy", "Charge Cycle", "Free Practice");
        on(Events.PacketReceive.class, e -> {
            if (!isEnabled() || mc.player == null) return;
            if (e.packet instanceof BlockUpdateS2CPacket p) mc.execute(() -> update(p.getPos(), p.getState()));
            else if (e.packet instanceof ChunkDeltaUpdateS2CPacket p) p.visitUpdates((pos, st) -> {
                BlockPos imm = pos.toImmutable();
                mc.execute(() -> update(imm, st));
            });
        });
    }

    @Override
    protected String mode() { return "Anchor"; }

    @Override
    protected boolean usesDummies() { return scenario.is("Static Dummy") || scenario.is("Moving Dummy"); }

    @Override
    protected double dummySpeed() { return scenario.is("Static Dummy") ? 0 : super.dummySpeed(); }

    @Override
    protected void onSessionStart() {
        placed.clear();
        charges.clear();
        glowstoneUsed = 0;
        if (mc.world != null && mc.world.getRegistryKey() == net.minecraft.world.World.NETHER) {
            voidclient.gui.Notifications.warning(getName(), "Anchors do not explode in the Nether - use the Overworld or End");
        }
    }

    @Override
    protected String placementLabel() { return "Anchors Placed"; }

    @Override
    protected void extraRows(List<String[]> rows) { rows.add(new String[]{"Glowstone Used", String.valueOf(glowstoneUsed)}); }

    private void update(BlockPos pos, BlockState state) {
        if (!running() || mc.player == null || pos.getSquaredDistance(mc.player.getEntityPos()) > 8 * 8) return;
        boolean anchor = state.isOf(Blocks.RESPAWN_ANCHOR);
        if (anchor) {
            int c = state.get(RespawnAnchorBlock.CHARGES);
            Integer prev = charges.put(pos, c);
            if (prev == null && !placed.containsKey(pos)) {
                placed.put(pos, System.currentTimeMillis());
                placements++;
            } else if (prev != null && c > prev) {
                glowstoneUsed += c - prev;
            }
        } else if (charges.containsKey(pos)) {
            Integer prev = charges.remove(pos);
            Long when = placed.remove(pos);
            if (prev != null && prev > 0) {
                if (when != null) cycles.add(System.currentTimeMillis() - when);
                if (scenario.is("Charge Cycle")) {
                    detonations++;
                    hits++;
                    hitSound.play();
                } else detonated();
            }
        }
    }

    @Override
    protected void sessionTick(long elapsedMs) {
        placed.keySet().removeIf(p -> System.currentTimeMillis() - placed.get(p) > 30_000);
    }
}
