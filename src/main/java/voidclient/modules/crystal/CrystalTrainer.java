package voidclient.modules.crystal;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.events.Events;
import voidclient.modules.training.TrainerBase;
import voidclient.render.RenderManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Crystal practice: dummy scenarios measure place-to-break timing, accuracy and damage;
 * Speed Placement highlights spots to hit as fast as possible. You place and break every
 * crystal yourself.
 */
public class CrystalTrainer extends TrainerBase {
    private final Map<Integer, Long> crystalSpawn = new HashMap<>();
    private final Map<Integer, BlockPos> crystalBase = new HashMap<>();
    private BlockPos targetSpot;
    private final Random random = new Random();

    public CrystalTrainer() {
        super("Crystal Practice Trainer", "Practice crystal timing and accuracy on dummies or speed drills.", Category.CRYSTAL,
                "Static Dummy", "Moving Dummy", "Multi-Target", "Speed Placement", "Free Practice");
        on(Events.AttackEntity.class, e -> {
            if (!running() || !(e.target() instanceof EndCrystalEntity c)) return;
            Long spawn = crystalSpawn.remove(c.getId());
            if (spawn != null) cycles.add(System.currentTimeMillis() - spawn);
            if (!scenario.is("Speed Placement")) detonated();
        });
        on(Events.WorldRender.class, e -> {
            if (!running() || targetSpot == null) return;
            float p = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 150.0));
            RenderManager.box(new Box(targetSpot).expand(0.01), 0x403CFFA0 | ((int) (p * 80) << 24), 0xFF3CFFA0, 3f, true);
            RenderManager.text("TARGET", Vec3d.ofCenter(targetSpot).add(0, 1.2, 0), 0.3f, 0xFF3CFFA0, 0x90000000, true);
        });
    }

    @Override
    protected String mode() { return "Crystal"; }

    @Override
    protected boolean usesDummies() { return scenario.is("Static Dummy") || scenario.is("Moving Dummy") || scenario.is("Multi-Target"); }

    @Override
    protected double dummySpeed() { return scenario.is("Static Dummy") ? 0 : super.dummySpeed(); }

    @Override
    protected int dummyCount() { return scenario.is("Multi-Target") ? Math.max(2, super.dummyCount() + 1) : 1; }

    @Override
    protected boolean bedrockArena() { return false; }

    @Override
    protected void onSessionStart() {
        crystalSpawn.clear();
        crystalBase.clear();
        targetSpot = scenario.is("Speed Placement") ? pickSpot() : null;
    }

    @Override
    protected String placementLabel() { return "Crystals Placed"; }

    private BlockPos pickSpot() {
        if (mc.player == null || mc.world == null) return null;
        List<BlockPos> spots = new ArrayList<>();
        BlockPos c = mc.player.getBlockPos();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -2; y <= 1; y++) {
            BlockPos p = c.add(x, y, z);
            var st = mc.world.getBlockState(p);
            if ((st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK)) && mc.world.getBlockState(p.up()).isAir() && mc.world.getBlockState(p.up(2)).isAir()
                    && p.up().getSquaredDistance(mc.player.getEyePos()) < 25 && !p.equals(targetSpot)) spots.add(p);
        }
        return spots.isEmpty() ? null : spots.get(random.nextInt(spots.size()));
    }

    @Override
    protected void sessionTick(long elapsedMs) {
        if (mc.world == null || mc.player == null) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity) || crystalSpawn.containsKey(e.getId()) || e.age > 5) continue;
            if (e.squaredDistanceTo(mc.player) > 7 * 7) continue;
            crystalSpawn.put(e.getId(), System.currentTimeMillis());
            BlockPos base = e.getBlockPos().down();
            crystalBase.put(e.getId(), base);
            placements++;
            if (scenario.is("Speed Placement")) {
                detonated();
                if (base.equals(targetSpot)) {
                    hits++;
                    hitSound.play();
                    hitThisWindow = true;
                    targetSpot = pickSpot();
                }
            }
        }
        crystalSpawn.keySet().removeIf(id -> mc.world.getEntityById(id) == null && System.currentTimeMillis() - crystalSpawn.get(id) > 3000);
        if (scenario.is("Speed Placement") && targetSpot == null && elapsedMs % 1000 < 50) targetSpot = pickSpot();
    }
}
