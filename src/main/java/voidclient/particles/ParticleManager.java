package voidclient.particles;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import voidclient.VoidClient;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Client-side world particle engine used by Custom/Ambient/Hit particles, trails, aura and
 * explosion effects. Hard capped; ticks at 20 TPS and renders interpolated billboards.
 */
public final class ParticleManager {
    private static final ParticleManager INSTANCE = new ParticleManager();
    private static final int HARD_CAP = 4000;

    public static ParticleManager get() { return INSTANCE; }

    private final List<VoidParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int cap = 1500;
    private boolean initialised;

    public void init() {
        if (initialised) return;
        initialised = true;
        var bus = VoidClient.get().events();
        bus.subscribe(Events.TickPost.class, e -> tick());
        bus.subscribe(Events.WorldRender.class, e -> render(e.tickDelta));
        bus.subscribe(Events.WorldLeave.class, e -> particles.clear());
    }

    public void setCap(int cap) { this.cap = Math.max(50, Math.min(HARD_CAP, cap)); }

    public int count() { return particles.size(); }

    public Random random() { return random; }

    public VoidParticle spawn(double x, double y, double z, double vx, double vy, double vz, float size, int color, int lifetime, float gravity) {
        if (particles.size() >= cap) particles.remove(0);
        VoidParticle p = new VoidParticle();
        p.x = x;
        p.y = y;
        p.z = z;
        p.vx = vx;
        p.vy = vy;
        p.vz = vz;
        p.size = size;
        p.color = color;
        p.lifetime = Math.max(1, lifetime);
        p.gravity = gravity;
        particles.add(p);
        return p;
    }

    /** Burst of particles in a sphere. */
    public void burst(Vec3d at, int amount, float speed, float size, int color, int lifetime, float gravity, boolean chroma) {
        for (int i = 0; i < amount; i++) {
            double th = random.nextDouble() * Math.PI * 2, ph = Math.acos(2 * random.nextDouble() - 1);
            double s = speed * (0.4 + random.nextDouble() * 0.6);
            int c = chroma ? ColorUtil.withAlpha(ColorUtil.chroma(random.nextFloat(), 0.8f, 1f), ColorUtil.alpha(color)) : color;
            spawn(at.x, at.y, at.z, Math.sin(ph) * Math.cos(th) * s, Math.cos(ph) * s, Math.sin(ph) * Math.sin(th) * s,
                    size * (0.6f + random.nextFloat() * 0.6f), c, (int) (lifetime * (0.7 + random.nextDouble() * 0.6)), gravity);
        }
    }

    private void tick() {
        if (MinecraftClient.getInstance().world == null) {
            particles.clear();
            return;
        }
        particles.removeIf(p -> !p.tick());
    }

    private void render(float tickDelta) {
        if (particles.isEmpty()) return;
        Vec3d cam = RenderManager.camera();
        for (VoidParticle p : particles) {
            double ix = p.x - p.vx * (1 - tickDelta), iy = p.y - p.vy * (1 - tickDelta), iz = p.z - p.vz * (1 - tickDelta);
            if (cam.squaredDistanceTo(ix, iy, iz) > 96 * 96) continue;
            float life = p.lifeFraction();
            float size = p.shrink ? p.size * (1f - life * 0.7f) : p.size;
            int c = ColorUtil.multiplyAlpha(p.color, 1f - life * life);
            RenderManager.billboard(new Vec3d(ix, iy, iz), size, c, false);
            if (p.glow) RenderManager.billboard(new Vec3d(ix, iy, iz), size * 2.2f, ColorUtil.multiplyAlpha(c, 0.25f), false);
        }
    }
}
