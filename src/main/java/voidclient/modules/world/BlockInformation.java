package voidclient.modules.world;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;

import java.util.List;
import java.util.Locale;

/** Details of the block under your crosshair. */
public class BlockInformation extends TextHudModule {
    public final BooleanSetting id = add(new BooleanSetting("Block ID", "Registry id.", true));
    public final BooleanSetting coordinates = add(new BooleanSetting("Block Coordinates", "Block coordinates.", true));
    public final BooleanSetting hardness = add(new BooleanSetting("Hardness", "Hardness and blast resistance.", true));
    public final BooleanSetting tool = add(new BooleanSetting("Harvestable", "Whether your held item mines it effectively.", true));
    public final BooleanSetting light = add(new BooleanSetting("Light", "Block and sky light of the face you look at.", true));
    public final BooleanSetting properties = add(new BooleanSetting("Properties", "Block state properties.", false));
    public final NumberSetting maxProperties = add(new NumberSetting("Max Properties", "Listed properties.", 4, 1, 12, 1).visibleWhen(properties::get));

    public BlockInformation() {
        super("Block Information", "Name, id, hardness, light and state of the targeted block.", Category.WORLD, 0.40, 0.08);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            if (editor) out.add(new Line("", "Stone"));
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        out.add(new Line("", state.getBlock().getName().getString(), accent()));
        if (id.get()) out.add(new Line("ID", Registries.BLOCK.getId(state.getBlock()).toString()));
        if (coordinates.get()) out.add(new Line("Pos", pos.getX() + " " + pos.getY() + " " + pos.getZ()));
        if (hardness.get()) out.add(new Line("Hardness", String.format(Locale.ROOT, "%.1f / %.0f", state.getHardness(mc.world, pos), state.getBlock().getBlastResistance())));
        if (tool.get() && mc.player != null) {
            boolean ok = !state.isToolRequired() || mc.player.getMainHandStack().isSuitableFor(state);
            out.add(new Line("Harvest", ok ? "Yes" : "No", ok ? 0xFF3CFFA0 : 0xFFFF3C6E));
        }
        if (light.get()) {
            BlockPos face = pos.offset(hit.getSide());
            out.add(new Line("Light", "block " + mc.world.getLightLevel(LightType.BLOCK, face) + "  sky " + mc.world.getLightLevel(LightType.SKY, face)));
        }
        if (properties.get()) {
            int n = 0;
            for (Property<?> p : state.getProperties()) {
                if (n++ >= maxProperties.getInt()) break;
                out.add(new Line(p.getName(), String.valueOf(state.get(p))));
            }
        }
    }
}
