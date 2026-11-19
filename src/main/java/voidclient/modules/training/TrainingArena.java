package voidclient.modules.training;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Practice arena and dummy targets. Only works in singleplayer: it runs on your own
 * integrated server. In multiplayer the trainers fall back to tracking-only mode.
 */
public final class TrainingArena {
    public static final String TAG = "void_dummy";
    private final List<UUID> dummies = new ArrayList<>();
    private Vec3d origin;

    public static boolean available() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.isInSingleplayer() && mc.getServer() != null && mc.player != null;
    }

    private static ServerWorld serverWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        IntegratedServer s = mc.getServer();
        if (s == null || mc.world == null) return null;
        return s.getWorld(mc.world.getRegistryKey());
    }

    public Vec3d origin() { return origin; }

    /** Builds a floor of the given block (obsidian / bedrock) and clears air above it. Modifies your world. */
    public void buildPlatform(int radius, boolean bedrock) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!available()) return;
        BlockPos base = mc.player.getBlockPos().down();
        origin = Vec3d.ofBottomCenter(base.up());
        var floor = bedrock ? Blocks.BEDROCK.getDefaultState() : Blocks.OBSIDIAN.getDefaultState();
        mc.getServer().execute(() -> {
            ServerWorld w = serverWorld();
            if (w == null) return;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                w.setBlockState(base.add(x, 0, z), floor);
                for (int y = 1; y <= 5; y++) {
                    BlockPos p = base.add(x, y, z);
                    if (!w.getBlockState(p).isAir() && !(x == 0 && z == 0 && y <= 2)) w.setBlockState(p, Blocks.AIR.getDefaultState());
                }
            }
        });
    }

    /** Spawns dummy targets around the player. Armour tier: 0 none, 1 iron, 2 diamond, 3 netherite. */
    public void spawnDummies(int count, double distance, int armorTier) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!available()) return;
        Vec3d center = mc.player.getEntityPos();
        if (origin == null) origin = center;
        float yaw = mc.player.getYaw();
        mc.getServer().execute(() -> {
            ServerWorld w = serverWorld();
            if (w == null) return;
            for (int i = 0; i < count; i++) {
                HuskEntity husk = EntityType.HUSK.create(w, SpawnReason.COMMAND);
                if (husk == null) continue;
                double rad = Math.toRadians(yaw);
                double fx = -Math.sin(rad), fz = Math.cos(rad), rx = Math.cos(rad), rz = Math.sin(rad);
                double side = (i - (count - 1) / 2.0) * 2.5;
                husk.refreshPositionAndAngles(center.x + fx * distance + rx * side, center.y, center.z + fz * distance + rz * side, yaw + 180, 0);
                husk.setAiDisabled(true);
                husk.setPersistent();
                husk.setSilent(true);
                husk.setCustomName(Text.literal("Void Dummy"));
                husk.setCustomNameVisible(true);
                husk.addCommandTag(TAG);
                ItemStack[] armor = switch (armorTier) {
                    case 1 -> new ItemStack[]{new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS)};
                    case 2 -> new ItemStack[]{new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
                    case 3 -> new ItemStack[]{new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)};
                    default -> new ItemStack[0];
                };
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (int k = 0; k < armor.length; k++) {
                    husk.equipStack(slots[k], armor[k]);
                    husk.setEquipmentDropChance(slots[k], 0f);
                }
                w.spawnEntity(husk);
                synchronized (dummies) {
                    dummies.add(husk.getUuid());
                }
            }
        });
    }

    /** Moves dummies along a circle (server side) so targets strafe. speed in blocks/s. */
    public void moveDummies(double speed, long timeMs) {
        if (!available() || speed <= 0 || origin == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d o = origin;
        mc.getServer().execute(() -> {
            ServerWorld w = serverWorld();
            if (w == null) return;
            List<UUID> ids;
            synchronized (dummies) {
                ids = new ArrayList<>(dummies);
            }
            int i = 0;
            for (UUID id : ids) {
                var e = w.getEntity(id);
                if (e == null) continue;
                double r = 4 + i * 1.5;
                double ang = timeMs / 1000.0 * speed / r + i * Math.PI;
                e.refreshPositionAndAngles(o.x + Math.cos(ang) * r, e.getY(), o.z + Math.sin(ang) * r, (float) Math.toDegrees(ang), 0);
                i++;
            }
        });
    }

    public int aliveDummies() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return 0;
        int n = 0;
        for (var e : mc.world.getEntities()) if (isDummy(e)) n++;
        return n;
    }

    public static boolean isDummy(net.minecraft.entity.Entity e) {
        return e instanceof HuskEntity && e.hasCustomName() && "Void Dummy".equals(e.getCustomName().getString());
    }

    public void clearDummies() {
        if (!available()) {
            dummies.clear();
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getServer().execute(() -> {
            ServerWorld w = serverWorld();
            if (w == null) return;
            List<UUID> ids;
            synchronized (dummies) {
                ids = new ArrayList<>(dummies);
                dummies.clear();
            }
            for (UUID id : ids) {
                var e = w.getEntity(id);
                if (e != null) e.discard();
            }
        });
    }
}
