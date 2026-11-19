package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.VoidClient;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.particles.ParticleManager;

/** Neon burst on your target when your hit lands (or on every swing). */
public class HitParticles extends ParticleModule {
    public final BooleanSetting onlyLanded = add(new BooleanSetting("Only Landed Hits", "Burst only when the target loses health.", true));
    public final NumberSetting speed = add(new NumberSetting("Burst Speed", "Initial particle speed.", 0.25, 0.02, 1, 0.01));
    public final BooleanSetting chroma = add(new BooleanSetting("Rainbow Burst", "Random hues per particle.", false));

    public HitParticles() {
        super("Hit Particles", "Particle burst on the entity you hit.");
        amount.defaultTo(16.0);
        on(Events.AttackEntity.class, e -> {
            if (!onlyLanded.get()) burst(e.target().getBoundingBox().getCenter());
        });
        on(Events.HealthChange.class, e -> {
            var c = VoidClient.get().combat();
            if (onlyLanded.get() && e.newHealth() < e.oldHealth() && e.entity() == c.lastTarget() && c.sinceAttackMs() < 600) {
                burst(e.entity().getBoundingBox().getCenter());
            }
        });
    }

    private void burst(Vec3d at) {
        ParticleManager.get().burst(at, amount.getInt(), speed.getFloat(), size.getFloat(), particleColor(0), lifetime.getInt(), gravity.getFloat(), chroma.get());
    }
}
