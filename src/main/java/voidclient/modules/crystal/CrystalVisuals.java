package voidclient.modules.crystal;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.modules.training.TrainingArena;
import voidclient.particles.ParticleManager;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.ExplosionMath;

import java.util.Locale;

/**
 * Crystal visuals: crystal outlines, placement preview with damage estimates (target and self),
 * animated target indicator and neon explosion particles. Purely visual.
 */
public class CrystalVisuals extends Module {
    public final BooleanSetting crystalOutline = add(new BooleanSetting("Crystal Outline", "Box around end crystals.", true));
    public final ColorSetting crystalColor = add(new ColorSetting("Crystal Color", "Crystal outline colour.", 0xFFB026FF).themed());
    public final BooleanSetting placementPreview = add(new BooleanSetting("Placement Preview", "Ghost crystal where you would place it.", true));
    public final ColorSetting previewColor = add(new ColorSetting("Preview Color", "Valid placement colour.", 0x6000F0FF));
    public final BooleanSetting damageInfo = add(new BooleanSetting("Damage Information", "Estimated damage to the target and to you.", true));
    public final BooleanSetting targetIndicator = add(new BooleanSetting("Target Indicator", "Animated ring under your current target.", true));
    public final ColorSetting targetColor = add(new ColorSetting("Target Color", "Target ring colour.", 0xFFFF2BD6));
    public final BooleanSetting particles = add(new BooleanSetting("Custom Particles", "Neon burst when crystals explode.", true));
    public final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Speed of pulses and rings.", 1, 0.2, 4, 0.1));
    public final NumberSetting range = add(new NumberSetting("Range", "Crystal outline range.", 12, 4, 32, 1).unit("m"));

    public CrystalVisuals() {
        super("Crystal Combat Visuals", "Crystal outlines, placement preview, damage estimates and target ring.", Category.CRYSTAL);
        on(Events.Explosion.class, e -> {
            if (particles.get() && mc.player != null && e.pos().distanceTo(mc.player.getEntityPos()) < 32) {
                ParticleManager.get().burst(e.pos(), 30, 0.4f, 0.1f, crystalColor.getColor(), 22, 0.003f, false);
            }
        });
        on(Events.WorldRender.class, this::render);
    }

    private LivingEntity target() {
        LivingEntity t = VoidClient.get().combat().lastTarget();
        if (t != null && VoidClient.get().combat().sinceAttackMs() < 10_000) return t;
        if (mc.world == null || mc.player == null) return null;
        LivingEntity best = null;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity le && (TrainingArena.isDummy(e) || e instanceof net.minecraft.entity.player.PlayerEntity) && e != mc.player
                    && (best == null || e.squaredDistanceTo(mc.player) < best.squaredDistanceTo(mc.player)) && e.squaredDistanceTo(mc.player) < 144) best = le;
        }
        return best;
    }

    private void render(Events.WorldRender e) {
        if (mc.world == null || mc.player == null) return;
        double t = System.currentTimeMillis() / 1000.0 * animationSpeed.get();
        if (crystalOutline.get()) {
            for (Entity ent : mc.world.getEntities()) {
                if (!(ent instanceof EndCrystalEntity) || ent.distanceTo(mc.player) > range.get()) continue;
                Vec3d p = ent.getLerpedPos(e.tickDelta);
                Box b = ent.getBoundingBox().offset(p.subtract(ent.getEntityPos()));
                float pulse = (float) (0.6 + 0.4 * Math.sin(t * 4 + ent.getId()));
                RenderManager.boxOutline(b, ColorUtil.multiplyAlpha(crystalColor.getColor(), pulse), 2f, true);
            }
        }
        LivingEntity target = target();
        if (targetIndicator.get() && target != null) {
            Vec3d base = target.getLerpedPos(e.tickDelta).add(0, 0.05, 0);
            double r = target.getWidth() * 0.9 + 0.1 * Math.sin(t * 3);
            RenderManager.ring(base, r, targetColor.getColor(), 3f, 48, false);
            RenderManager.ring(base.add(0, target.getHeight() * (0.5 + 0.5 * Math.sin(t * 2)), 0), r * 0.85, ColorUtil.multiplyAlpha(targetColor.getColor(), 0.5f), 2f, 48, false);
        }
        if (placementPreview.get() && (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) || mc.player.getOffHandStack().isOf(Items.END_CRYSTAL))
                && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos base = hit.getBlockPos();
            var st = mc.world.getBlockState(base);
            boolean valid = (st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK)) && mc.world.getBlockState(base.up()).isAir()
                    && mc.world.getOtherEntities(null, new Box(base.up()).stretch(0, 1, 0)).isEmpty();
            int c = valid ? previewColor.getColor() : 0x60FF3C3C;
            Box ghost = new Box(base.getX() + 0.0, base.getY() + 1.0, base.getZ() + 0.0, base.getX() + 1.0, base.getY() + 3.0, base.getZ() + 1.0);
            RenderManager.box(ghost, c, ColorUtil.withAlpha(c, 255), 2f, false);
            if (valid && damageInfo.get()) {
                Vec3d center = Vec3d.ofBottomCenter(base.up());
                float self = ExplosionMath.estimate(mc.player, center, ExplosionMath.CRYSTAL);
                String label = String.format(Locale.ROOT, "self ~%.1f", self);
                if (target != null) label = String.format(Locale.ROOT, "target ~%.1f  |  ", ExplosionMath.estimate(target, center, ExplosionMath.CRYSTAL)) + label;
                float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                RenderManager.text(Text.literal(label), center.add(0, 2.4, 0), 0.22f, self >= hp ? 0xFFFF3C3C : 0xFFFFFFFF, 0x90000000, true, true);
            }
        }
    }
}
