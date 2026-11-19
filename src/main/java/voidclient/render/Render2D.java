package voidclient.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import voidclient.utilities.ColorUtil;

/**
 * Vector 2D renderer on top of the GUI render state: sub-pixel positions, anti-aliased
 * rounded rectangles, gradients, glows, circles and arcs. All sizes are in GUI units, so the
 * output scales cleanly with GUI scale and resolution (1080p - 4K).
 */
public final class Render2D {
    private Render2D() {}

    /**
     * Vanilla GUI pipeline with back-face culling disabled: fans and feathered rings are not
     * wound like vanilla quads, and the default pipeline culls them.
     */
    public static final com.mojang.blaze3d.pipeline.RenderPipeline SHAPES = RenderPipelines.register(
            com.mojang.blaze3d.pipeline.RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.of("voidclient", "pipeline/gui_shapes"))
                    .withCull(false)
                    .build());

    /** Width of the anti-aliasing feather in GUI units. */
    private static final float AA = 0.75f;

    // ---- vertex builder -----------------------------------------------------------------

    private static final class Builder {
        float[] d = new float[384];
        int n;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        void reset() {
            n = 0;
            minX = minY = Float.MAX_VALUE;
            maxX = maxY = -Float.MAX_VALUE;
        }

        void v(float x, float y, int c) {
            if ((n + 1) * 3 > d.length) d = java.util.Arrays.copyOf(d, d.length * 2);
            int i = n * 3;
            d[i] = x;
            d[i + 1] = y;
            d[i + 2] = Float.intBitsToFloat(c);
            n++;
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }

        void quad(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
            v(x1, y1, c1);
            v(x2, y2, c2);
            v(x3, y3, c3);
            v(x4, y4, c4);
        }

        void tri(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
            quad(x1, y1, c1, x1, y1, c1, x2, y2, c2, x3, y3, c3);
        }
    }

    private static final Builder B = new Builder();

    private static void flush(DrawContext ctx) {
        if (B.n == 0) return;
        Matrix3x2f pose = new Matrix3x2f(ctx.getMatrices());
        ScreenRect scissor = ctx.scissorStack.peekLast();
        int bx = (int) Math.floor(B.minX), by = (int) Math.floor(B.minY);
        ScreenRect bounds = new ScreenRect(bx, by, Math.max(1, (int) Math.ceil(B.maxX) - bx), Math.max(1, (int) Math.ceil(B.maxY) - by))
                .transformEachVertex(pose);
        if (scissor != null) bounds = scissor.intersection(bounds);
        if (bounds == null) {
            B.reset();
            return;
        }
        float[] copy = java.util.Arrays.copyOf(B.d, B.n * 3);
        ctx.state.addSimpleElement(new ShapeElement(SHAPES, TextureSetup.empty(), pose, copy, B.n, scissor, bounds));
        B.reset();
    }

    // ---- rectangles ---------------------------------------------------------------------

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0 || ColorUtil.alpha(color) == 0) return;
        B.quad(x, y, color, x, y + h, color, x + w, y + h, color, x + w, y, color);
        flush(ctx);
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        if (w <= 0 || h <= 0) return;
        B.quad(x, y, left, x, y + h, left, x + w, y + h, right, x + w, y, right);
        flush(ctx);
    }

    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        B.quad(x, y, top, x, y + h, bottom, x + w, y + h, bottom, x + w, y, top);
        flush(ctx);
    }

    public static void outline(DrawContext ctx, float x, float y, float w, float h, float t, int color) {
        rect(ctx, x, y, w, t, color);
        rect(ctx, x, y + h - t, w, t, color);
        rect(ctx, x, y + t, t, h - 2 * t, color);
        rect(ctx, x + w - t, y + t, t, h - 2 * t, color);
    }

    // ---- rounded rectangles -------------------------------------------------------------

    /** Colour of a point inside a 4-corner gradient (tl, tr, br, bl). */
    private static int bilerp(int tl, int tr, int br, int bl, float u, float v) {
        return ColorUtil.lerp(ColorUtil.lerp(tl, tr, u), ColorUtil.lerp(bl, br, u), v);
    }

    private static int segments(float r) {
        return Math.max(3, Math.min(16, (int) Math.ceil(r * 1.6f)));
    }

    public static void roundRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        roundRectGradient(ctx, x, y, w, h, r, color, color, color, color);
    }

    public static void roundRectH(DrawContext ctx, float x, float y, float w, float h, float r, int left, int right) {
        roundRectGradient(ctx, x, y, w, h, r, left, right, right, left);
    }

    public static void roundRectV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        roundRectGradient(ctx, x, y, w, h, r, top, top, bottom, bottom);
    }

    /**
     * Filled anti-aliased rounded rectangle with a 4-corner gradient. Built as a fan of
     * triangles from the centre to an outline polygon, plus a feathered outer ring.
     */
    public static void roundRectGradient(DrawContext ctx, float x, float y, float w, float h, float r, int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float[] pts = outlinePoints(x, y, w, h, r, 0);
        float[] outer = outlinePoints(x, y, w, h, r, AA);
        int count = pts.length / 2;
        float cx = x + w / 2f, cy = y + h / 2f;
        int cc = bilerp(tl, tr, br, bl, 0.5f, 0.5f);
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            float ax = pts[i * 2], ay = pts[i * 2 + 1], bx = pts[j * 2], by = pts[j * 2 + 1];
            int ca = bilerp(tl, tr, br, bl, (ax - x) / w, (ay - y) / h);
            int cb = bilerp(tl, tr, br, bl, (bx - x) / w, (by - y) / h);
            B.tri(cx, cy, cc, ax, ay, ca, bx, by, cb);
            float oax = outer[i * 2], oay = outer[i * 2 + 1], obx = outer[j * 2], oby = outer[j * 2 + 1];
            B.quad(ax, ay, ca, oax, oay, ColorUtil.withAlpha(ca, 0), obx, oby, ColorUtil.withAlpha(cb, 0), bx, by, cb);
        }
        flush(ctx);
    }

    /** Anti-aliased rounded border of thickness t drawn inside the rectangle. */
    public static void roundRectOutline(DrawContext ctx, float x, float y, float w, float h, float r, float t, int color) {
        roundRectOutlineGradient(ctx, x, y, w, h, r, t, color, color);
    }

    public static void roundRectOutlineGradient(DrawContext ctx, float x, float y, float w, float h, float r, float t, int c1, int c2) {
        if (w <= 0 || h <= 0 || t <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float[] mid = outlinePoints(x, y, w, h, r, -t / 2f);
        int count = mid.length / 2;
        float half = t / 2f;
        float[] in = outlinePoints(x, y, w, h, r, -t);
        float[] out = outlinePoints(x, y, w, h, r, 0);
        float[] inAA = outlinePoints(x, y, w, h, r, -t - AA);
        float[] outAA = outlinePoints(x, y, w, h, r, AA);
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            float ui = (mid[i * 2] - x) / w, uj = (mid[j * 2] - x) / w;
            int ci = ColorUtil.lerp(c1, c2, ui), cj = ColorUtil.lerp(c1, c2, uj);
            int ti = ColorUtil.withAlpha(ci, 0), tj = ColorUtil.withAlpha(cj, 0);
            B.quad(in[i * 2], in[i * 2 + 1], ci, out[i * 2], out[i * 2 + 1], ci, out[j * 2], out[j * 2 + 1], cj, in[j * 2], in[j * 2 + 1], cj);
            B.quad(out[i * 2], out[i * 2 + 1], ci, outAA[i * 2], outAA[i * 2 + 1], ti, outAA[j * 2], outAA[j * 2 + 1], tj, out[j * 2], out[j * 2 + 1], cj);
            B.quad(inAA[i * 2], inAA[i * 2 + 1], ti, in[i * 2], in[i * 2 + 1], ci, in[j * 2], in[j * 2 + 1], cj, inAA[j * 2], inAA[j * 2 + 1], tj);
        }
        if (half < 0) return;
        flush(ctx);
    }

    /**
     * Soft drop shadow / neon glow around a rounded rectangle: a ring that fades from
     * {@code color} at the rectangle edge to transparent at {@code spread} units outside.
     */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, float spread, int color) {
        if (w <= 0 || h <= 0 || spread <= 0 || ColorUtil.alpha(color) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float[] in = outlinePoints(x, y, w, h, r, 0);
        float[] out = outlinePoints(x, y, w, h, r, spread);
        int count = in.length / 2;
        int clear = ColorUtil.withAlpha(color, 0);
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            B.quad(in[i * 2], in[i * 2 + 1], color, out[i * 2], out[i * 2 + 1], clear, out[j * 2], out[j * 2 + 1], clear, in[j * 2], in[j * 2 + 1], color);
        }
        flush(ctx);
    }

    /** Neon glow: layered shadows for a bloom-like halo. */
    public static void glow(DrawContext ctx, float x, float y, float w, float h, float r, float strength, int color) {
        if (strength <= 0) return;
        shadow(ctx, x, y, w, h, r, 3 * strength, ColorUtil.multiplyAlpha(color, 0.55f));
        shadow(ctx, x, y, w, h, r, 9 * strength, ColorUtil.multiplyAlpha(color, 0.25f));
    }

    /**
     * Points along a rounded rectangle outline, expanded outward by {@code expand}
     * (negative shrinks). Corners are emitted clockwise starting at the top-left.
     */
    private static float[] outlinePoints(float x, float y, float w, float h, float r, float expand) {
        float er = Math.max(0, r + expand);
        int seg = r <= 0 ? 1 : segments(r);
        float[] p = new float[4 * (seg + 1) * 2];
        float[][] centers = {
                {x + r, y + r, (float) Math.PI},
                {x + w - r, y + r, (float) (Math.PI * 1.5)},
                {x + w - r, y + h - r, 0f},
                {x + r, y + h - r, (float) (Math.PI * 0.5)}
        };
        int k = 0;
        for (float[] c : centers) {
            for (int s = 0; s <= seg; s++) {
                double a = c[2] + (Math.PI / 2) * s / seg;
                double cos = Math.cos(a), sin = Math.sin(a);
                if (r <= 0) {
                    // square corner: push diagonally so the expansion stays uniform
                    cos = Math.signum(Math.round(Math.cos(c[2] + Math.PI / 4) * 1000));
                    sin = Math.signum(Math.round(Math.sin(c[2] + Math.PI / 4) * 1000));
                    p[k++] = (float) (c[0] + cos * expand);
                    p[k++] = (float) (c[1] + sin * expand);
                    continue;
                }
                p[k++] = (float) (c[0] + cos * er);
                p[k++] = (float) (c[1] + sin * er);
            }
        }
        return p;
    }

    // ---- circles, arcs, lines -----------------------------------------------------------

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        int seg = Math.max(12, Math.min(64, (int) (radius * 3)));
        int clear = ColorUtil.withAlpha(color, 0);
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2 * i / seg, a2 = Math.PI * 2 * (i + 1) / seg;
            float x1 = cx + (float) Math.cos(a1) * radius, y1 = cy + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius, y2 = cy + (float) Math.sin(a2) * radius;
            B.tri(cx, cy, color, x1, y1, color, x2, y2, color);
            float ox1 = cx + (float) Math.cos(a1) * (radius + AA), oy1 = cy + (float) Math.sin(a1) * (radius + AA);
            float ox2 = cx + (float) Math.cos(a2) * (radius + AA), oy2 = cy + (float) Math.sin(a2) * (radius + AA);
            B.quad(x1, y1, color, ox1, oy1, clear, ox2, oy2, clear, x2, y2, color);
        }
        flush(ctx);
    }

    /** Soft elliptical halo: full colour at the centre fading linearly to transparent at the rim. */
    public static void radialGlow(DrawContext ctx, float cx, float cy, float rx, float ry, int color) {
        if (rx <= 0 || ry <= 0) return;
        int seg = 48;
        int clear = ColorUtil.withAlpha(color, 0);
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2 * i / seg, a2 = Math.PI * 2 * (i + 1) / seg;
            B.tri(cx, cy, color, cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, clear,
                    cx + (float) Math.cos(a2) * rx, cy + (float) Math.sin(a2) * ry, clear);
        }
        flush(ctx);
    }

    /** Arc ring from startDeg sweeping sweepDeg (clockwise, 0 = up). */
    public static void arc(DrawContext ctx, float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, int color) {
        if (radius <= 0 || thickness <= 0 || Math.abs(sweepDeg) < 0.01f) return;
        int seg = Math.max(4, Math.min(96, (int) (Math.abs(sweepDeg) / 6f) + 2));
        float ri = radius - thickness / 2f, ro = radius + thickness / 2f;
        int clear = ColorUtil.withAlpha(color, 0);
        for (int i = 0; i < seg; i++) {
            double a1 = Math.toRadians(startDeg + sweepDeg * i / seg - 90), a2 = Math.toRadians(startDeg + sweepDeg * (i + 1) / seg - 90);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1), c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
            B.quad(cx + c1 * ri, cy + s1 * ri, color, cx + c1 * ro, cy + s1 * ro, color, cx + c2 * ro, cy + s2 * ro, color, cx + c2 * ri, cy + s2 * ri, color);
            B.quad(cx + c1 * ro, cy + s1 * ro, color, cx + c1 * (ro + AA), cy + s1 * (ro + AA), clear, cx + c2 * (ro + AA), cy + s2 * (ro + AA), clear, cx + c2 * ro, cy + s2 * ro, color);
            B.quad(cx + c1 * (ri - AA), cy + s1 * (ri - AA), clear, cx + c1 * ri, cy + s1 * ri, color, cx + c2 * ri, cy + s2 * ri, color, cx + c2 * (ri - AA), cy + s2 * (ri - AA), clear);
        }
        flush(ctx);
    }

    public static void ring(DrawContext ctx, float cx, float cy, float radius, float thickness, int color) {
        arc(ctx, cx, cy, radius, thickness, 0, 360, color);
    }

    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, float width, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = -dy / len * width / 2f, ny = dx / len * width / 2f;
        float ax = -dy / len * (width / 2f + AA), ay = dx / len * (width / 2f + AA);
        int clear = ColorUtil.withAlpha(color, 0);
        B.quad(x1 + nx, y1 + ny, color, x1 - nx, y1 - ny, color, x2 - nx, y2 - ny, color, x2 + nx, y2 + ny, color);
        B.quad(x1 + nx, y1 + ny, color, x1 + ax, y1 + ay, clear, x2 + ax, y2 + ay, clear, x2 + nx, y2 + ny, color);
        B.quad(x1 - ax, y1 - ay, clear, x1 - nx, y1 - ny, color, x2 - nx, y2 - ny, color, x2 - ax, y2 - ay, clear);
        flush(ctx);
    }

    /** Filled triangle (used for arrows / chevrons). */
    public static void triangle(DrawContext ctx, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        B.tri(x1, y1, color, x2, y2, color, x3, y3, color);
        flush(ctx);
    }

    // ---- textures ------------------------------------------------------------------------

    /** Draws a full texture stretched to (w,h) at a sub-pixel position, tinted by color. */
    public static void texture(DrawContext ctx, Identifier tex, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(w / 256f, h / 256f);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, 256, 256, 256, 256, 256, 256, color);
        m.popMatrix();
    }

    /** Draws a texture region (u,v,regionW,regionH of a texW x texH texture) into (x,y,w,h). */
    public static void textureRegion(DrawContext ctx, Identifier tex, float x, float y, float w, float h,
                                     float u, float v, int regionW, int regionH, int texW, int texH, int color) {
        if (w <= 0 || h <= 0 || regionW <= 0 || regionH <= 0) return;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(w / regionW, h / regionH);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, 0, 0, u, v, regionW, regionH, regionW, regionH, texW, texH, color);
        m.popMatrix();
    }

    // ---- scissor -------------------------------------------------------------------------

    public static void scissor(DrawContext ctx, float x, float y, float w, float h) {
        ctx.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
    }

    public static void endScissor(DrawContext ctx) { ctx.disableScissor(); }
}
