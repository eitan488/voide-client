package voidclient.utilities;

/** Pure math helpers. */
public final class MathUtil {
    private MathUtil() {}

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(hi, v); }
    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : Math.min(hi, v); }
    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(hi, v); }

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static boolean isFinite(double... values) {
        for (double v : values) if (Double.isNaN(v) || Double.isInfinite(v)) return false;
        return true;
    }

    /** Frame-rate independent exponential approach: speed is 1/s. */
    public static double approach(double current, double target, double speed, double dtSeconds) {
        double k = 1.0 - Math.exp(-speed * Math.max(0, dtSeconds));
        return current + (target - current) * k;
    }

    public static double round(double v, int places) {
        double s = Math.pow(10, places);
        return Math.round(v * s) / s;
    }

    public static String formatDuration(long millis) {
        long s = Math.max(0, millis) / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    public static String formatTicks(int ticks) { return formatDuration(ticks * 50L); }

    /** Wraps degrees into [-180, 180). */
    public static float wrapDegrees(float deg) {
        float d = deg % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static String cardinal(float yaw) {
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = Math.floorMod(Math.round(yaw / 45f), 8);
        return dirs[i];
    }
}
