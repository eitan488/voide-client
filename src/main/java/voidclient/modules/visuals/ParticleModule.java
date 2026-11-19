package voidclient.modules.visuals;

import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.particles.ParticleManager;
import voidclient.particles.VoidParticle;
import voidclient.utilities.ColorUtil;

/**
 * Base for Void particle effects. Every subclass gets the full particle setting set:
 * type, amount, size, lifetime, gravity, density, colour, rotation, transparency, quality.
 */
public abstract class ParticleModule extends Module {
    public final ModeSetting type = add(new ModeSetting("Type", "Particle behaviour.", "Neon Dust", "Neon Dust", "Sparkles", "Embers", "Snow", "Bubbles", "Stars"));
    public final NumberSetting amount = add(new NumberSetting("Amount", "Particles per emission.", 4, 1, 40, 1));
    public final NumberSetting size = add(new NumberSetting("Size", "Particle size (blocks).", 0.08, 0.01, 0.5, 0.01));
    public final NumberSetting lifetime = add(new NumberSetting("Lifetime", "Ticks a particle lives.", 30, 5, 200, 1));
    public final NumberSetting gravity = add(new NumberSetting("Gravity", "Downward acceleration (negative floats up).", 0.002, -0.02, 0.05, 0.001));
    public final NumberSetting density = add(new NumberSetting("Density", "Emission chance per tick.", 0.6, 0.05, 1, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", "Particle colour.", 0xFFB026FF).themed());
    public final NumberSetting rotation = add(new NumberSetting("Rotation", "Swirl speed.", 0, 0, 1, 0.05));
    public final NumberSetting transparency = add(new NumberSetting("Transparency", "0 = opaque, 1 = invisible.", 0.2, 0, 0.95, 0.05));
    public final ModeSetting quality = add(new ModeSetting("Quality", "Glow halo and cap.", "High", "Low", "Medium", "High"));

    protected ParticleModule(String name, String description) {
        super(name, description, Category.VISUALS);
    }

    protected int particleColor(float offset) {
        int c = color.getColor(offset);
        return ColorUtil.multiplyAlpha(c, 1f - transparency.getFloat());
    }

    /** Emits one particle with this module's style at pos with base velocity. */
    protected void emit(Vec3d pos, double vx, double vy, double vz, float spread) {
        ParticleManager pm = ParticleManager.get();
        var r = pm.random();
        float s = size.getFloat();
        float g = gravity.getFloat();
        int life = lifetime.getInt();
        double jx = (r.nextDouble() - 0.5) * spread, jy = (r.nextDouble() - 0.5) * spread, jz = (r.nextDouble() - 0.5) * spread;
        switch (type.get()) {
            case "Embers" -> {
                vy += 0.03 + r.nextDouble() * 0.03;
                g = -Math.abs(g) * 0.5f;
            }
            case "Snow" -> {
                vy -= 0.01;
                g = Math.abs(g) * 0.3f;
            }
            case "Bubbles" -> {
                vy += 0.02;
                g = -0.001f;
            }
            case "Sparkles" -> s *= 0.6f;
            case "Stars" -> s *= 1.3f;
            default -> {
            }
        }
        VoidParticle p = pm.spawn(pos.x + jx * 0.3, pos.y + jy * 0.3, pos.z + jz * 0.3, vx + jx * 0.05, vy + jy * 0.05, vz + jz * 0.05, s,
                particleColor(r.nextFloat() * 0.3f), (int) (life * (0.7 + r.nextDouble() * 0.6)), g);
        p.glow = quality.is("High") || (quality.is("Medium") && r.nextBoolean());
        p.spin = rotation.getFloat() * 20f;
        if (rotation.get() > 0) {
            double a = r.nextDouble() * Math.PI * 2;
            p.vx += Math.cos(a) * rotation.get() * 0.05;
            p.vz += Math.sin(a) * rotation.get() * 0.05;
        }
    }

    protected boolean roll() { return ParticleManager.get().random().nextDouble() < density.get(); }
}
