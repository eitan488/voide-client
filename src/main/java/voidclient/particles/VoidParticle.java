package voidclient.particles;

/** A simple world-space billboard particle (camera-facing square with fade). */
public final class VoidParticle {
    public double x, y, z, vx, vy, vz;
    public float size, gravity, drag = 0.98f, rotation, spin;
    public int color, age, lifetime;
    public boolean shrink = true, glow;

    public boolean tick() {
        age++;
        vy -= gravity;
        vx *= drag;
        vy *= drag;
        vz *= drag;
        x += vx;
        y += vy;
        z += vz;
        rotation += spin;
        return age < lifetime;
    }

    public float lifeFraction() { return Math.min(1f, age / (float) Math.max(1, lifetime)); }
}
