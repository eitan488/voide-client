package voidclient.modules.movement;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.utilities.MathUtil;

import java.util.List;
import java.util.Locale;

/** XYZ with nether/overworld conversion, facing and chunk. */
public class Coordinates extends TextHudModule {
    public final NumberSetting decimals = add(new NumberSetting("Decimals", "Decimal places.", 1, 0, 3, 1));
    public final BooleanSetting otherDimension = add(new BooleanSetting("Other Dimension", "Show the equivalent Nether / Overworld position.", true));
    public final BooleanSetting facing = add(new BooleanSetting("Facing", "Cardinal direction and axis.", true));
    public final BooleanSetting chunk = add(new BooleanSetting("Chunk", "Chunk coordinates.", false));
    public final BooleanSetting compact = add(new BooleanSetting("Compact", "Single XYZ line.", true));

    public Coordinates() {
        super("Coordinates", "Your position with dimension conversion.", Category.MOVEMENT, 0.005, 0.20);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("XYZ", "128.5 64.0 -342.5"));
            return;
        }
        String f = "%." + decimals.getInt() + "f";
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (compact.get()) out.add(new Line("XYZ", String.format(Locale.ROOT, f + " " + f + " " + f, x, y, z)));
        else {
            out.add(new Line("X", String.format(Locale.ROOT, f, x)));
            out.add(new Line("Y", String.format(Locale.ROOT, f, y)));
            out.add(new Line("Z", String.format(Locale.ROOT, f, z)));
        }
        if (otherDimension.get() && mc.world != null) {
            boolean nether = mc.world.getRegistryKey() == World.NETHER;
            if (nether || mc.world.getRegistryKey() == World.OVERWORLD) {
                double k = nether ? 8 : 0.125;
                out.add(new Line(nether ? "Overworld" : "Nether", String.format(Locale.ROOT, f + " " + f, x * k, z * k), 0xFFFF7A5C));
            }
        }
        if (facing.get()) {
            String axis = switch (mc.player.getHorizontalFacing()) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case WEST -> "-X";
                default -> "+X";
            };
            out.add(new Line("Facing", MathUtil.cardinal(mc.player.getYaw()) + " (" + axis + ")"));
        }
        if (chunk.get()) {
            ChunkPos c = mc.player.getChunkPos();
            out.add(new Line("Chunk", c.x + ", " + c.z));
        }
    }
}
