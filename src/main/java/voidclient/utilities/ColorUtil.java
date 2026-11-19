package voidclient.utilities;

/** Pure ARGB colour math (no Minecraft dependencies, unit-testable). */
public final class ColorUtil {
    private ColorUtil() {}

    public static int alpha(int c) { return (c >>> 24) & 0xFF; }
    public static int red(int c) { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c) { return c & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }

    private static int clamp255(int v) { return v < 0 ? 0 : Math.min(255, v); }

    public static int withAlpha(int c, int a) { return (clamp255(a) << 24) | (c & 0xFFFFFF); }

    public static int multiplyAlpha(int c, float f) {
        return withAlpha(c, Math.round(alpha(c) * Math.max(0f, Math.min(1f, f))));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
                Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
                Math.round(red(a) + (red(b) - red(a)) * t),
                Math.round(green(a) + (green(b) - green(a)) * t),
                Math.round(blue(a) + (blue(b) - blue(a)) * t));
    }

    public static int brighten(int c, float f) {
        return argb(alpha(c), Math.round(red(c) + (255 - red(c)) * f), Math.round(green(c) + (255 - green(c)) * f), Math.round(blue(c) + (255 - blue(c)) * f));
    }

    public static int darken(int c, float f) {
        float k = 1f - f;
        return argb(alpha(c), Math.round(red(c) * k), Math.round(green(c) * k), Math.round(blue(c) * k));
    }

    /** HSB to ARGB, h in [0,1). */
    public static int hsb(float h, float s, float b) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF);
    }

    public static float[] toHsb(int c) {
        return java.awt.Color.RGBtoHSB(red(c), green(c), blue(c), null);
    }

    /** Time-based rainbow; offset shifts the hue so lists can cascade. */
    public static int chroma(float offset, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() % 6000L) / 6000f + offset) % 1f;
        if (hue < 0) hue += 1f;
        return hsb(hue, saturation, brightness);
    }

    /** Health fraction to red-yellow-green. */
    public static int health(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return hsb(fraction / 3f, 0.85f, 0.95f);
    }

    public static int parseHex(String s, int fallback) {
        if (s == null) return fallback;
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        try {
            if (h.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(h, 16);
            if (h.length() == 8) return (int) Long.parseLong(h, 16);
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    public static String toHex(int c, boolean withAlpha) {
        return withAlpha ? String.format("#%08X", c) : String.format("#%06X", c & 0xFFFFFF);
    }
}
