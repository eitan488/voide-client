package voidclient.gui.animations;

/** Global animation preferences, written by the Animation Settings module. */
public final class AnimationManager {
    private static volatile boolean enabled = true;
    private static volatile double speed = 1.0;
    private static volatile Easing easing = Easing.CUBIC_OUT;

    private AnimationManager() {}

    public static void configure(boolean on, double speedMultiplier, Easing curve) {
        enabled = on;
        speed = Math.max(0.1, speedMultiplier);
        easing = curve;
    }

    public static boolean enabled() { return enabled; }
    public static double speed() { return speed; }
    public static Easing easing() { return easing; }

    /** Converts a base duration (ms) to the effective duration. */
    public static double duration(double baseMs) { return enabled ? baseMs / speed : 0; }
}
