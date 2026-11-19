package voidclient.modules.visuals;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.player.PlayerEntity;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.particles.ParticleManager;

/** Particles from any entity that takes damage (vanilla particle types or Void neon). */
public class DamageParticles extends ParticleModule {
    public final ModeSetting source = add(new ModeSetting("Particle Source", "Vanilla particle type or Void neon particles.", "Void Neon", "Void Neon", "Crit", "Magic Crit", "Damage Hearts", "Heart"));
    public final NumberSetting multiplier = add(new NumberSetting("Per Damage", "Particles per point of damage.", 2, 0.5, 10, 0.5));
    public final BooleanSetting onlyPlayers = add(new BooleanSetting("Only Players", "Only for players.", false));

    public DamageParticles() {
        super("Damage Particles", "Extra particles when entities take damage.");
        on(Events.HealthChange.class, e -> {
            float dmg = e.oldHealth() - e.newHealth();
            if (dmg <= 0 || mc.world == null || (onlyPlayers.get() && !(e.entity() instanceof PlayerEntity))) return;
            int n = Math.min(60, Math.max(1, Math.round(dmg * multiplier.getFloat())));
            var box = e.entity().getBoundingBox();
            var r = ParticleManager.get().random();
            if (source.is("Void Neon")) {
                ParticleManager.get().burst(box.getCenter(), n, 0.2f, size.getFloat(), particleColor(0), lifetime.getInt(), gravity.getFloat(), false);
                return;
            }
            ParticleEffect effect = switch (source.get()) {
                case "Magic Crit" -> ParticleTypes.ENCHANTED_HIT;
                case "Damage Hearts" -> ParticleTypes.DAMAGE_INDICATOR;
                case "Heart" -> ParticleTypes.HEART;
                default -> ParticleTypes.CRIT;
            };
            for (int i = 0; i < n; i++) {
                double x = box.minX + r.nextDouble() * box.getLengthX(), y = box.minY + r.nextDouble() * box.getLengthY(), z = box.minZ + r.nextDouble() * box.getLengthZ();
                mc.world.addParticleClient(effect, x, y, z, (r.nextDouble() - 0.5) * 0.4, r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.4);
            }
        });
    }
}
