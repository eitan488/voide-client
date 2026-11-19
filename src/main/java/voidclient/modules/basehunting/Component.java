package voidclient.modules.basehunting;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.registry.tag.FluidTags;

import java.util.function.Predicate;

/** Block components counted by chunk inspection, grouped as in the inspection spec. */
public enum Component {
    REDSTONE_DUST("Redstone Dust", Group.REDSTONE, s -> s.isOf(Blocks.REDSTONE_WIRE)),
    REPEATER("Repeater", Group.REDSTONE, s -> s.isOf(Blocks.REPEATER)),
    COMPARATOR("Comparator", Group.REDSTONE, s -> s.isOf(Blocks.COMPARATOR)),
    REDSTONE_TORCH("Redstone Torch", Group.REDSTONE, s -> s.isOf(Blocks.REDSTONE_TORCH) || s.isOf(Blocks.REDSTONE_WALL_TORCH)),
    REDSTONE_BLOCK("Redstone Block", Group.REDSTONE, s -> s.isOf(Blocks.REDSTONE_BLOCK)),
    OBSERVER("Observer", Group.REDSTONE, s -> s.isOf(Blocks.OBSERVER)),
    PISTON("Piston", Group.MECHANISMS, s -> s.isOf(Blocks.PISTON)),
    STICKY_PISTON("Sticky Piston", Group.MECHANISMS, s -> s.isOf(Blocks.STICKY_PISTON)),
    HOPPER("Hopper", Group.MECHANISMS, s -> s.isOf(Blocks.HOPPER)),
    DISPENSER("Dispenser", Group.MECHANISMS, s -> s.isOf(Blocks.DISPENSER)),
    DROPPER("Dropper", Group.MECHANISMS, s -> s.isOf(Blocks.DROPPER)),
    NOTE_BLOCK("Note Block", Group.MECHANISMS, s -> s.isOf(Blocks.NOTE_BLOCK)),
    TARGET_BLOCK("Target Block", Group.MECHANISMS, s -> s.isOf(Blocks.TARGET)),
    CHEST("Chest", Group.STORAGE, s -> s.isOf(Blocks.CHEST)),
    TRAPPED_CHEST("Trapped Chest", Group.STORAGE, s -> s.isOf(Blocks.TRAPPED_CHEST)),
    BARREL("Barrel", Group.STORAGE, s -> s.isOf(Blocks.BARREL)),
    SHULKER_BOX("Shulker Box", Group.STORAGE, s -> s.getBlock() instanceof ShulkerBoxBlock),
    ENDER_CHEST("Ender Chest", Group.STORAGE, s -> s.isOf(Blocks.ENDER_CHEST)),
    WATER_STREAM("Water Stream", Group.FARM, s -> s.getFluidState().isIn(FluidTags.WATER) && !s.getFluidState().isStill()),
    COLLECTION("Collection System", Group.FARM, s -> false);

    public enum Group {
        REDSTONE("Redstone", 0xFFFF3C3C), MECHANISMS("Mechanisms", 0xFFFFC93C), STORAGE("Storage", 0xFF3CFFA0), FARM("Farm Components", 0xFF3CB4FF);

        public final String label;
        public final int color;

        Group(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    public final String label;
    public final Group group;
    public final Predicate<BlockState> matcher;

    Component(String label, Group group, Predicate<BlockState> matcher) {
        this.label = label;
        this.group = group;
        this.matcher = matcher;
    }

    public static Component byLabel(String label) {
        for (Component c : values()) if (c.label.equals(label)) return c;
        return null;
    }

    public static Component match(BlockState s) {
        if (s.isAir()) return null;
        for (Component c : values()) if (c.matcher.test(s)) return c;
        return null;
    }
}
