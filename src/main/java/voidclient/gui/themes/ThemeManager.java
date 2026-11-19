package voidclient.gui.themes;

import voidclient.utilities.ColorUtil;

/**
 * Global access to the active palette. The Theme Engine module pushes the selected theme
 * (or the user's custom theme) here; everything else only reads.
 */
public final class ThemeManager {
    private static volatile Theme current = Theme.CYBERPUNK;
    private static volatile boolean animated = true;
    private static volatile float speed = 1f;
    private static volatile float glow = 1f;
    private static volatile float panelOpacity = 1f;

    private ThemeManager() {}

    public static Theme get() { return current; }

    public static void apply(Theme theme, boolean animatedGradient, float gradientSpeed, float glowStrength, float opacity) {
        current = theme;
        animated = animatedGradient;
        speed = gradientSpeed;
        glow = glowStrength;
        panelOpacity = opacity;
    }

    public static float glowStrength() { return glow; }

    /** Panel colours with the user's opacity multiplier applied. */
    public static int panel(int color) { return ColorUtil.multiplyAlpha(color, panelOpacity); }

    /**
     * Animated neon accent: travels accent1 -> accent2 -> accent3 -> accent1.
     * {@code offset} (0..1) shifts the phase so neighbouring elements cascade.
     */
    public static int accent(float offset) {
        Theme t = current;
        float phase = animated ? (System.currentTimeMillis() % (long) (6000 / Math.max(0.1f, speed))) / (6000f / Math.max(0.1f, speed)) : 0f;
        float p = ((phase + offset) % 1f + 1f) % 1f * 3f;
        if (p < 1f) return ColorUtil.lerp(t.accent1(), t.accent2(), smooth(p));
        if (p < 2f) return ColorUtil.lerp(t.accent2(), t.accent3(), smooth(p - 1f));
        return ColorUtil.lerp(t.accent3(), t.accent1(), smooth(p - 2f));
    }

    private static float smooth(float t) { return t * t * (3f - 2f * t); }

    public static int accent1() { return current.accent1(); }
    public static int accent2() { return current.accent2(); }
}
