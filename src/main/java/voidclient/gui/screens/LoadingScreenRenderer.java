package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/**
 * Loading overlay drawn only with vector shapes (no textures, no fonts), because it also runs
 * during the very first resource reload before any assets exist.
 */
public final class LoadingScreenRenderer {
    public static final int BACKGROUND = 0xFF03030A;

    private LoadingScreenRenderer() {}

    public static void background(DrawContext ctx, int w, int h, float alpha) {
        Render2D.rect(ctx, 0, 0, w, h, ColorUtil.multiplyAlpha(BACKGROUND, alpha));
        Render2D.gradientV(ctx, 0, h * 0.55f, w, h * 0.45f, 0x00000000, ColorUtil.multiplyAlpha(ColorUtil.withAlpha(ThemeManager.get().accent1(), 40), alpha));
    }

    public static void foreground(DrawContext ctx, int w, int h, float alpha, float progress, float barAlpha) {
        if (alpha <= 0.01f) return;
        Theme t = ThemeManager.get();
        double time = (System.currentTimeMillis() % 100_000L) / 1000.0;
        float cx = w / 2f, cy = h * 0.45f;
        float r = Math.min(w, h) * 0.09f;
        int a1 = ColorUtil.multiplyAlpha(t.accent1(), alpha), a2 = ColorUtil.multiplyAlpha(t.accent2(), alpha), a3 = ColorUtil.multiplyAlpha(t.accent3(), alpha);
        // glow halo
        for (int i = 0; i < 4; i++) Render2D.ring(ctx, cx, cy, r + i * 3, 6, ColorUtil.multiplyAlpha(a1, 0.08f));
        Render2D.ring(ctx, cx, cy, r, 2.2f, ColorUtil.multiplyAlpha(a2, 0.35f));
        // spinning arcs
        float spin = (float) (time * 160 % 360);
        Render2D.arc(ctx, cx, cy, r, 3.2f, spin, 110, a1);
        Render2D.arc(ctx, cx, cy, r, 3.2f, spin + 180, 110, a2);
        Render2D.arc(ctx, cx, cy, r * 0.72f, 2f, -spin * 1.4f, 70, a3);
        // V glyph
        float vw = r * 0.55f, vh = r * 0.55f;
        Render2D.line(ctx, cx - vw, cy - vh * 0.6f, cx, cy + vh * 0.7f, r * 0.12f, a1);
        Render2D.line(ctx, cx, cy + vh * 0.7f, cx + vw, cy - vh * 0.6f, r * 0.12f, a2);
        // progress bar
        if (barAlpha > 0.01f) {
            float bw = Math.min(w * 0.42f, 360), bh = 4;
            float bx = cx - bw / 2f, by = h * 0.72f;
            int track = ColorUtil.multiplyAlpha(0x30FFFFFF, barAlpha);
            Render2D.roundRect(ctx, bx, by, bw, bh, 2, track);
            float pw = Math.max(bh, bw * progress);
            Render2D.glow(ctx, bx, by, pw, bh, 2, 0.8f, ColorUtil.multiplyAlpha(t.accent1(), barAlpha));
            Render2D.roundRectH(ctx, bx, by, pw, bh, 2, ColorUtil.multiplyAlpha(t.accent1(), barAlpha), ColorUtil.multiplyAlpha(t.accent2(), barAlpha));
            float shimmer = (float) ((time * 0.8) % 1.0);
            Render2D.gradientH(ctx, bx + pw * shimmer - 12, by, 12, bh, 0x00FFFFFF, ColorUtil.multiplyAlpha(0x80FFFFFF, barAlpha));
            // segment ticks
            for (int i = 1; i < 10; i++) Render2D.rect(ctx, bx + bw * i / 10f, by + bh + 3, 0.6f, 2, ColorUtil.multiplyAlpha(0x40FFFFFF, barAlpha));
        }
    }
}
