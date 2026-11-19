package voidclient.modules.anchor;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.ExplosionMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Charge state of nearby respawn anchors with glowstone pips, blast radius and self-damage. */
public class AnchorVisualizer extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", "Scan radius around you.", 8, 3, 16, 1).unit("m"));
    public final ColorSetting emptyColor = add(new ColorSetting("Empty Color", "Uncharged anchor colour.", 0xFFB026FF));
    public final ColorSetting chargedColor = add(new ColorSetting("Charged Color", "Charged anchor colour.", 0xFFFF7A2E));
    public final NumberSetting transparency = add(new NumberSetting("Transparency", "Fill transparency.", 0.75, 0, 0.98, 0.02));
    public final BooleanSetting glowstonePips = add(new BooleanSetting("Glowstone Indicator", "Show charge pips above anchors.", true));
    public final BooleanSetting blastRadius = add(new BooleanSetting("Blast Radius", "Ring for charged anchors you look at.", true));
    public final BooleanSetting damage = add(new BooleanSetting("Self Damage", "Estimated damage if the looked-at anchor explodes.", true));
    public final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Pulse speed of charged anchors.", 1, 0.2, 4, 0.1));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw through blocks.", true));

    private final List<BlockPos> anchors = new ArrayList<>();
    private int ticks;

    public AnchorVisualizer() {
        super("Anchor Visualizer", "Anchor charge states, glowstone pips, blast radius and damage estimate.", Category.ANCHOR);
        on(Events.TickPost.class, e -> {
            if (++ticks % 4 != 0 || mc.world == null || mc.player == null) return;
            anchors.clear();
            BlockPos c = mc.player.getBlockPos();
            int r = range.getInt();
            for (BlockPos p : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
                if (mc.world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)) anchors.add(p.toImmutable());
            }
        });
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null) return;
            double t = System.currentTimeMillis() / 1000.0 * animationSpeed.get();
            for (BlockPos p : anchors) {
                BlockState st = mc.world.getBlockState(p);
                if (!st.isOf(Blocks.RESPAWN_ANCHOR)) continue;
                int ch = st.get(RespawnAnchorBlock.CHARGES);
                float pulse = ch > 0 ? (float) (0.55 + 0.45 * Math.sin(t * 5)) : 1f;
                int c = ColorUtil.lerp(emptyColor.getColor(), chargedColor.getColor(), ch / 4f);
                RenderManager.box(new Box(p).expand(0.005), ColorUtil.multiplyAlpha(c, (1 - transparency.getFloat()) * pulse), c, 2f, throughWalls.get());
                if (glowstonePips.get()) {
                    StringBuilder pips = new StringBuilder();
                    for (int i = 0; i < 4; i++) pips.append(i < ch ? '◆' : '◇');
                    RenderManager.text(Text.literal(pips + " " + ch + "/4"), Vec3d.ofCenter(p).add(0, 0.9, 0), 0.2f, ch > 0 ? 0xFFFFC93C : 0xFFCCCCCC, 0x90000000, throughWalls.get(), true);
                }
            }
            if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockState st = mc.world.getBlockState(hit.getBlockPos());
                if (st.isOf(Blocks.RESPAWN_ANCHOR) && st.get(RespawnAnchorBlock.CHARGES) > 0) {
                    Vec3d center = Vec3d.ofCenter(hit.getBlockPos());
                    if (blastRadius.get()) RenderManager.ring(center.add(0, -0.45, 0), ExplosionMath.ANCHOR * 2, ColorUtil.withAlpha(chargedColor.getColor(), 170), 2f, 64, false);
                    if (damage.get()) {
                        float d = ExplosionMath.estimate(mc.player, center, ExplosionMath.ANCHOR);
                        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                        RenderManager.text(Text.literal(String.format(Locale.ROOT, "self ~%.1f dmg", d)), center.add(0, 1.4, 0), 0.22f, d >= hp ? 0xFFFF3C3C : 0xFFFFFFFF, 0x90000000, true, true);
                    }
                }
            }
        });
    }
}
