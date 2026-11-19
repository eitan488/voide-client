package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Floating ambient motes (fireflies / dust) in the air around the camera. */
public class AmbientParticles extends ParticleModule {
    public final NumberSetting range = add(new NumberSetting("Range", "Spawn distance around the camera.", 14, 4, 40, 1));

    public AmbientParticles() {
        super("Ambient Particles", "Ambient floating particles that fill the air around you.");
        type.defaultTo("Stars");
        gravity.defaultTo(-0.0005);
        lifetime.defaultTo(120.0);
        on(Events.TickPost.class, e -> {
            if (mc.player == null || !roll()) return;
            var r = voidclient.particles.ParticleManager.get().random();
            Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
            for (int i = 0; i < amount.getInt(); i++) {
                Vec3d p = cam.add((r.nextDouble() - 0.5) * 2 * range.get(), (r.nextDouble() - 0.3) * range.get(), (r.nextDouble() - 0.5) * 2 * range.get());
                if (!mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(p)).isAir()) continue;
                emit(p, (r.nextDouble() - 0.5) * 0.01, 0, (r.nextDouble() - 0.5) * 0.01, 0.1f);
            }
        });
    }
}
