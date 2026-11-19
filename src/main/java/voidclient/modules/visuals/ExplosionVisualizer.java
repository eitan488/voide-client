package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.particles.ParticleManager;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.ExplosionMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Blast radii and estimated self-damage for crystals and TNT; flashes real explosions. */
public class ExplosionVisualizer extends Module {
    private record Flash(Vec3d pos, float radius, long time) {}

    public final BooleanSetting crystals = add(new BooleanSetting("End Crystals", "Visualise end crystals.", true));
    public final BooleanSetting tnt = add(new BooleanSetting("TNT", "Visualise primed TNT and TNT minecarts.", true));
    public final BooleanSetting damage = add(new BooleanSetting("Self Damage", "Estimated damage to you if it exploded now.", true));
    public final ColorSetting radiusColor = add(new ColorSetting("Radius Color", "Blast radius colour.", 0xA0FF3C6E));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Range.", 16, 4, 64, 1).unit("m"));
    public final BooleanSetting flashes = add(new BooleanSetting("Explosion Flash", "Expanding ring when an explosion packet arrives.", true));
    public final BooleanSetting burst = add(new BooleanSetting("Explosion Particles", "Neon burst on explosions.", true));

    private final List<Flash> flashList = new ArrayList<>();

    public ExplosionVisualizer() {
        super("Explosion Visualizer", "Blast radius and damage estimates for crystals and TNT.", Category.VISUALS);
        alsoListIn(Category.CRYSTAL);
        on(Events.Explosion.class, e -> {
            if (flashes.get()) flashList.add(new Flash(e.pos(), e.radius(), System.currentTimeMillis()));
            if (burst.get()) ParticleManager.get().burst(e.pos(), 40, 0.5f, 0.12f, radiusColor.getColor() | 0xFF000000, 25, 0.004f, false);
        });
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null) return;
            for (Entity ent : mc.world.getEntities()) {
                float power;
                if (crystals.get() && ent instanceof EndCrystalEntity) power = ExplosionMath.CRYSTAL;
                else if (tnt.get() && (ent instanceof TntEntity || ent instanceof TntMinecartEntity)) power = ExplosionMath.TNT;
                else continue;
                if (ent.distanceTo(mc.player) > maxDistance.get()) continue;
                Vec3d c = ent.getLerpedPos(e.tickDelta);
                float r = power * 2;
                RenderManager.ring(c.add(0, 0.05, 0), r, radiusColor.getColor(), 2f, 64, false);
                RenderManager.ring(c.add(0, 0.05, 0), r / 2, ColorUtil.multiplyAlpha(radiusColor.getColor(), 0.5f), 1.5f, 48, false);
                if (damage.get()) {
                    float dmg = ExplosionMath.estimate(mc.player, c, power);
                    float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                    int col = dmg >= hp ? 0xFFFF2020 : dmg > hp / 2 ? 0xFFFFC93C : 0xFF3CFFA0;
                    RenderManager.text(Text.literal(String.format(Locale.ROOT, "~%.1f dmg", dmg)), c.add(0, ent.getHeight() + 0.6, 0), 0.2f, col, 0x90000000, true, true);
                }
            }
            long now = System.currentTimeMillis();
            flashList.removeIf(f -> now - f.time() > 700);
            for (Flash f : flashList) {
                float t = (now - f.time()) / 700f;
                RenderManager.ring(f.pos(), f.radius() * 2 * t, ColorUtil.multiplyAlpha(radiusColor.getColor(), 1 - t), 4f, 64, false);
            }
        });
    }
}
