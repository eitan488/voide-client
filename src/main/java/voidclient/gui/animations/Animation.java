package voidclient.gui.animations;

/**
 * Time-based tween from the current value to a target. Retargeting mid-flight starts a new
 * tween from the present value, so motion stays continuous.
 */
public final class Animation {
    private double from, to, value;
    private long startNanos;
    private final double baseDurationMs;
    private final Easing easingOverride;

    public Animation(double initial, double durationMs) { this(initial, durationMs, null); }

    public Animation(double initial, double durationMs, Easing easing) {
        this.from = this.to = this.value = initial;
        this.baseDurationMs = durationMs;
        this.easingOverride = easing;
        this.startNanos = System.nanoTime();
    }

    public void target(double target) {
        if (target == to) return;
        update();
        from = value;
        to = target;
        startNanos = System.nanoTime();
    }

    public void snap(double v) {
        from = to = value = v;
    }

    public double update() {
        double dur = AnimationManager.duration(baseDurationMs);
        if (dur <= 0) {
            value = to;
            return value;
        }
        double t = (System.nanoTime() - startNanos) / 1e6 / dur;
        if (t >= 1) {
            value = to;
        } else {
            Easing e = easingOverride != null ? easingOverride : AnimationManager.easing();
            value = from + (to - from) * e.apply(Math.max(0, t));
        }
        return value;
    }

    public float get() { return (float) update(); }
    public double target() { return to; }
    public boolean isDone() { update(); return value == to; }
}
