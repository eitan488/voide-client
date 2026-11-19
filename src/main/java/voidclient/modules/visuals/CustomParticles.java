package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Configurable particles emitted around your player. */
public class CustomParticles extends ParticleModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", "Emission radius around you.", 0.8, 0.1, 4, 0.1));
    public final BooleanSetting onlyMoving = add(new BooleanSetting("Only While Moving", "Emit only when moving.", false));

    public CustomParticles() {
        super("Custom Particles", "Custom particle effects around your character.");
        on(Events.TickPost.class, e -> {
            if (mc.player == null || (onlyMoving.get() && mc.player.getVelocity().horizontalLengthSquared() < 1e-4)) return;
            if (!roll()) return;
            var r = voidclient.particles.ParticleManager.get().random();
            for (int i = 0; i < amount.getInt(); i++) {
                double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * radius.get();
                Vec3d p = mc.player.getEntityPos().add(Math.cos(a) * d, r.nextDouble() * mc.player.getHeight(), Math.sin(a) * d);
                emit(p, 0, 0.01, 0, 0.5f);
            }
        });
    }
}
