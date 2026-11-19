package voidclient.modules.world;

import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/** Overlay of block light on walkable surfaces; highlights spots where mobs can spawn. */
public class LightLevel extends Module {
    private record Spot(BlockPos pos, int light) {}

    public final ModeSetting mode = add(new ModeSetting("Mode", "Show only dark spawnable spots, or every level.", "Spawnable", "Spawnable", "All Levels"));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Horizontal scan radius.", 16, 4, 32, 1).unit("m"));
    public final NumberSetting threshold = add(new NumberSetting("Danger Threshold", "Block light at or below this is dangerous (1.18+ = 0).", 0, 0, 7, 1));
    public final ColorSetting danger = add(new ColorSetting("Danger Color", "Spawnable spot colour.", 0x80FF2040));
    public final ColorSetting safe = add(new ColorSetting("Safe Color", "Lit spot colour (All Levels mode).", 0x5040FF80).visibleWhen(() -> mode.is("All Levels")));
    public final BooleanSetting numbers = add(new BooleanSetting("Numbers", "Draw the light value on each block.", false));
    public final NumberSetting interval = add(new NumberSetting("Update Interval", "Ticks between rescans.", 10, 2, 60, 1));

    private final List<Spot> spots = new ArrayList<>();
    private int ticks;

    public LightLevel() {
        super("Light Level", "Shows dark spots where hostile mobs can spawn.", Category.WORLD);
        alsoListIn(Category.VISUALS);
        on(Events.TickPost.class, e -> {
            if (++ticks % interval.getInt() == 0) scan();
        });
        on(Events.WorldRender.class, e -> {
            for (Spot s : spots) {
                boolean bad = s.light() <= threshold.getInt();
                int c = bad ? danger.getColor() : safe.getColor();
                double x = s.pos().getX(), y = s.pos().getY() + 0.01, z = s.pos().getZ();
                RenderManager.flatQuad(x + 0.1, z + 0.1, x + 0.9, z + 0.9, y, c, false);
                if (numbers.get()) RenderManager.text(Text.literal(String.valueOf(s.light())), new Vec3d(x + 0.5, y + 0.3, z + 0.5), 0.02f, ColorUtil.withAlpha(c, 255), 0, false, false);
            }
        });
    }

    @Override
    protected void onDisable() { spots.clear(); }

    private void scan() {
        spots.clear();
        if (mc.world == null || mc.player == null) return;
        BlockPos center = mc.player.getBlockPos();
        int r = radius.getInt();
        BlockPos.Mutable pos = new BlockPos.Mutable(), below = new BlockPos.Mutable();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -6; y <= 4; y++) {
            pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
            BlockState here = mc.world.getBlockState(pos);
            if (!here.getCollisionShape(mc.world, pos).isEmpty() || !here.getFluidState().isEmpty()) continue;
            below.set(pos.getX(), pos.getY() - 1, pos.getZ());
            BlockState ground = mc.world.getBlockState(below);
            if (!ground.isSideSolidFullSquare(mc.world, below, Direction.UP)) continue;
            int light = mc.world.getLightLevel(LightType.BLOCK, pos);
            if (mode.is("Spawnable") && light > threshold.getInt()) continue;
            spots.add(new Spot(pos.toImmutable(), light));
            if (spots.size() > 6000) return;
        }
    }
}
