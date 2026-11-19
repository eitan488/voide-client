package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Random;

/** Animated menu backgrounds: Nebula, Aurora, Starfield, Synthwave, AMOLED. */
public final class MenuBackground {
    private static final Identifier NEBULA = Identifier.of("voidclient", "textures/gui/menu_background.png");
    private static final int MAX_PARTICLES = 300;
    private static final float[] PX = new float[MAX_PARTICLES], PY = new float[MAX_PARTICLES], PZ = new float[MAX_PARTICLES], PS = new float[MAX_PARTICLES];
    private static boolean seeded;
    private static long last = System.nanoTime();
    private static float mouseX = -1, mouseY = -1;

    private MenuBackground() {}

    private static void seed() {
        Random r = new Random(42);
        for (int i = 0; i < MAX_PARTICLES; i++) {
            PX[i] = r.nextFloat();
            PY[i] = r.nextFloat();
            PZ[i] = 0.2f + r.nextFloat() * 0.8f;
            PS[i] = r.nextFloat();
        }
        seeded = true;
    }

    public static void setMouse(float x, float y) {
        mouseX = x;
        mouseY = y;
    }

    public static void render(DrawContext ctx, float w, float h, float delta) {
        if (!seeded) seed();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        Theme t = ThemeManager.get();
        String mode = VoidClient.get().clientSettings().menuBackground.get();
        double time = (System.currentTimeMillis() % 1_000_000L) / 1000.0;
        Render2D.rect(ctx, 0, 0, w, h, 0xFF000000);
        switch (mode) {
            case "Nebula" -> {
                float drift = (float) Math.sin(time * 0.05) * 12;
                float s = Math.max(w / 1920f, h / 1080f) * 1.04f;
                Render2D.texture(ctx, NEBULA, (w - 1920 * s) / 2 + drift, (h - 1080 * s) / 2, 1920 * s, 1080 * s, 0xFFFFFFFF);
                Render2D.gradientV(ctx, 0, 0, w, h, 0x10000000, 0x90000000);
            }
            case "Aurora" -> {
                Render2D.gradientV(ctx, 0, 0, w, h, 0xFF02030A, 0xFF070214);
                for (int band = 0; band < 3; band++) {
                    int c = band == 0 ? t.accent1() : band == 1 ? t.accent2() : t.accent3();
                    int seg = 48;
                    for (int i = 0; i < seg; i++) {
                        float x0 = w * i / seg, x1 = w * (i + 1) / seg;
                        float y0 = (float) (h * (0.35 + band * 0.12) + Math.sin(i * 0.35 + time * (0.4 + band * 0.15)) * h * 0.08);
                        float y1 = (float) (h * (0.35 + band * 0.12) + Math.sin((i + 1) * 0.35 + time * (0.4 + band * 0.15)) * h * 0.08);
                        float yy = Math.min(y0, y1);
                        Render2D.gradientV(ctx, x0, yy - h * 0.12f, x1 - x0 + 0.5f, h * 0.24f, ColorUtil.withAlpha(c, 0), ColorUtil.withAlpha(c, 0));
                        Render2D.gradientV(ctx, x0, yy - h * 0.10f, x1 - x0 + 0.5f, h * 0.10f, ColorUtil.withAlpha(c, 0), ColorUtil.withAlpha(c, 70));
                        Render2D.gradientV(ctx, x0, yy, x1 - x0 + 0.5f, h * 0.10f, ColorUtil.withAlpha(c, 70), ColorUtil.withAlpha(c, 0));
                    }
                }
            }
            case "Synthwave" -> {
                Render2D.gradientV(ctx, 0, 0, w, h * 0.55f, 0xFF05010F, ColorUtil.withAlpha(t.accent3(), 255));
                Render2D.circle(ctx, w / 2f, h * 0.48f, h * 0.18f, ColorUtil.withAlpha(t.accent1(), 220));
                for (int i = 0; i < 6; i++) Render2D.rect(ctx, w / 2f - h * 0.2f, h * (0.42f + i * 0.025f), h * 0.4f, 1.5f + i * 0.6f, 0xFF05010F);
                Render2D.rect(ctx, 0, h * 0.55f, w, h * 0.45f, 0xFF05010F);
                float horizon = h * 0.55f;
                int grid = ColorUtil.withAlpha(t.accent2(), 160);
                for (int i = -20; i <= 20; i++) Render2D.line(ctx, w / 2f + i * w * 0.02f, horizon, w / 2f + i * w * 0.18f, h, 1f, grid);
                double phase = (time * 0.6) % 1.0;
                for (int i = 0; i < 12; i++) {
                    double z = (i + phase) / 12.0;
                    float yy = horizon + (float) (Math.pow(z, 2.2) * (h - horizon));
                    Render2D.rect(ctx, 0, yy, w, 1f, grid);
                }
            }
            case "AMOLED" -> Render2D.rect(ctx, 0, 0, w, h, 0xFF000000);
            default -> Render2D.gradientV(ctx, 0, 0, w, h, 0xFF010108, 0xFF060312);
        }
        int count = Math.min(MAX_PARTICLES, VoidClient.get().clientSettings().menuParticles.getInt());
        boolean stars = mode.equals("Starfield");
        for (int i = 0; i < (stars ? MAX_PARTICLES : count); i++) {
            if (stars) {
                PZ[i] -= dt * 0.12f;
                if (PZ[i] <= 0.02f) {
                    PZ[i] = 1f;
                    PX[i] = (float) Math.random();
                    PY[i] = (float) Math.random();
                }
                float sx = w / 2f + (PX[i] - 0.5f) * w / PZ[i];
                float sy = h / 2f + (PY[i] - 0.5f) * h / PZ[i];
                if (sx < -5 || sx > w + 5 || sy < -5 || sy > h + 5) {
                    PZ[i] = 1f;
                    continue;
                }
                float size = (1f - PZ[i]) * 2.2f + 0.3f;
                Render2D.circle(ctx, sx, sy, size, ColorUtil.withAlpha(0xFFFFFFFF, (int) (255 * (1f - PZ[i]))));
            } else {
                PY[i] -= dt * 0.012f * PZ[i];
                PX[i] += (float) Math.sin(time * 0.3 + i) * dt * 0.002f;
                if (PY[i] < -0.02f) {
                    PY[i] = 1.02f;
                    PX[i] = (float) Math.random();
                }
                float px = PX[i] * w, py = PY[i] * h;
                if (mouseX >= 0) {
                    float dx = px - mouseX, dy = py - mouseY;
                    float d2 = dx * dx + dy * dy;
                    if (d2 < 3600) {
                        float push = (1f - d2 / 3600f) * 6f;
                        px += dx / (float) Math.sqrt(d2 + 1) * push;
                        py += dy / (float) Math.sqrt(d2 + 1) * push;
                    }
                }
                int c = ColorUtil.lerp(t.accent1(), t.accent2(), PS[i]);
                float tw = (float) (0.5 + 0.5 * Math.sin(time * 2 + i * 1.7));
                Render2D.circle(ctx, px, py, 0.6f + PZ[i] * 1.4f, ColorUtil.withAlpha(c, (int) (80 + 140 * tw * PZ[i])));
            }
        }
    }
}
