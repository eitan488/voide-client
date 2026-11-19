package voidclient.modules.basehunting;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.events.Events;
import voidclient.utilities.ChunkTracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Containers in loaded chunks (block entities the server sent), colour-coded by type. */
public class StorageEsp extends EspModule {
    private record Target(BlockPos pos, String type) {}

    public final MultiSelectSetting types = add(new MultiSelectSetting("Types", "Container types to show.",
            List.of("Chest", "Trapped Chest", "Ender Chest", "Barrel", "Shulker Box", "Hopper", "Furnace", "Dispenser/Dropper", "Brewing Stand"),
            Set.of("Chest", "Trapped Chest", "Ender Chest", "Barrel", "Shulker Box")));

    private final List<Target> targets = new ArrayList<>();
    private int ticks;

    public StorageEsp() {
        super("Storage ESP", "Highlights chests, barrels, shulkers and other containers.");
        on(Events.TickPost.class, e -> {
            if (++ticks % 10 == 0) refresh();
        });
        on(Events.WorldRender.class, e -> {
            for (Target t : targets) {
                Vec3d c = Vec3d.ofCenter(t.pos());
                if (!inRange(c)) continue;
                int col = color(t.type());
                drawBox(new Box(t.pos()).contract(0.0625), col);
                drawLabel(t.type(), c.add(0, 0.8, 0), col);
            }
        });
    }

    private String type(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return "Trapped Chest";
        if (be instanceof ChestBlockEntity) return "Chest";
        if (be instanceof EnderChestBlockEntity) return "Ender Chest";
        if (be instanceof BarrelBlockEntity) return "Barrel";
        if (be instanceof ShulkerBoxBlockEntity) return "Shulker Box";
        if (be instanceof HopperBlockEntity) return "Hopper";
        if (be instanceof AbstractFurnaceBlockEntity) return "Furnace";
        if (be instanceof DispenserBlockEntity) return "Dispenser/Dropper";
        if (be instanceof BrewingStandBlockEntity) return "Brewing Stand";
        return null;
    }

    private int color(String type) {
        return switch (type) {
            case "Trapped Chest" -> 0xFFFF5A3C;
            case "Ender Chest" -> 0xFFB026FF;
            case "Barrel" -> 0xFFC8963C;
            case "Shulker Box" -> 0xFFFF2BD6;
            case "Hopper" -> 0xFF9AA0B8;
            case "Furnace" -> 0xFFFF8C3C;
            case "Dispenser/Dropper" -> 0xFF7A7A7A;
            case "Brewing Stand" -> 0xFF3CB4FF;
            default -> color.getColor();
        };
    }

    private void refresh() {
        targets.clear();
        if (mc.player == null) return;
        ChunkPos pc = mc.player.getChunkPos();
        int r = (int) Math.ceil(renderDistance.get() / 16.0);
        for (WorldChunk chunk : ChunkTracker.around(pc.x, pc.z, r)) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                String t = type(be);
                if (t != null && types.isSelected(t)) targets.add(new Target(be.getPos(), t));
            }
        }
        targets.sort(Comparator.comparingDouble(t -> t.pos().getSquaredDistance(mc.player.getEntityPos())));
        while (targets.size() > maxCount.getInt()) targets.remove(targets.size() - 1);
    }

    @Override
    public String getInfo() { return String.valueOf(targets.size()); }
}
