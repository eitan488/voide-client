package voidclient.modules.visuals;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

/** Replaces the black block outline with a coloured outline and/or fill. */
public class BlockOverlay extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Outline, fill or both.", "Both", "Outline", "Fill", "Both"));
    public final ColorSetting outline = add(new ColorSetting("Outline Color", "Outline colour.", 0xFFB026FF).themed());
    public final ColorSetting fill = add(new ColorSetting("Fill Color", "Fill colour.", 0x30B026FF));
    public final NumberSetting width = add(new NumberSetting("Line Width", "Outline thickness.", 2.5, 1, 8, 0.5));
    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", "Animate the fill alpha.", true));
    public final BooleanSetting fullBlock = add(new BooleanSetting("Full Block", "Outline the whole block instead of its shape.", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw on top of everything.", false));

    public BlockOverlay() {
        super("Block Overlay", "Custom outline and fill for the block you are looking at.", Category.VISUALS);
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, state) -> !isEnabled());
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            BlockPos pos = hit.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) return;
            VoxelShape shape = fullBlock.get() ? null : state.getOutlineShape(mc.world, pos);
            float a = pulse.get() ? (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 250.0)) : 1f;
            Iterable<Box> boxes = shape == null || shape.isEmpty() ? java.util.List.of(new Box(0, 0, 0, 1, 1, 1)) : shape.getBoundingBoxes();
            for (Box b : boxes) {
                Box world = b.offset(pos).expand(0.002);
                if (!mode.is("Outline")) RenderManager.boxFilled(world, ColorUtil.multiplyAlpha(fill.getColor(), a), throughWalls.get());
                if (!mode.is("Fill")) RenderManager.boxOutline(world, outline.getColor(), width.getFloat(), throughWalls.get());
            }
        });
    }
}
