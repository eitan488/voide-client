package voidclient.gui.components;

import net.minecraft.client.gui.DrawContext;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.FontRenderer.Family;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Shared look of the Void GUI: typography, toggles, buttons, chips, tooltips. */
public final class UI {
    private UI() {}

    public static final Family FONT = Family.INTER;
    public static final Family BOLD = Family.INTER_BOLD;

    public static Theme t() { return ThemeManager.get(); }

    public static float text(DrawContext ctx, String s, float x, float y, int color) {
        return FontRenderer.draw(ctx, s, x, y, color, FONT, false);
    }

    public static float bold(DrawContext ctx, String s, float x, float y, int color) {
        return FontRenderer.draw(ctx, s, x, y, color, BOLD, false);
    }

    public static float small(DrawContext ctx, String s, float x, float y, int color) {
        return FontRenderer.drawScaled(ctx, s, x, y, 0.75f, color, FONT, false);
    }

    public static float width(String s) { return FontRenderer.width(s, FONT); }
    public static float boldWidth(String s) { return FontRenderer.width(s, BOLD); }
    public static float smallWidth(String s) { return FontRenderer.width(s, FONT) * 0.75f; }

    public static void centered(DrawContext ctx, String s, float cx, float y, int color) {
        text(ctx, s, cx - width(s) / 2f, y, color);
    }

    public static String trim(String s, float max) { return FontRenderer.trim(s, max, FONT); }

    /** Animated pill switch; {@code anim} is the knob position 0..1. */
    public static void toggle(DrawContext ctx, float x, float y, float w, float h, float anim, boolean hover) {
        Theme t = t();
        int off = ColorUtil.withAlpha(t.textDim(), 70);
        int on = ThemeManager.accent(0);
        int track = ColorUtil.lerp(off, ColorUtil.withAlpha(on, 230), anim);
        if (anim > 0.05f) Render2D.glow(ctx, x, y, w, h, h / 2f, 0.6f * anim * ThemeManager.glowStrength(), on);
        Render2D.roundRectH(ctx, x, y, w, h, h / 2f, track, anim > 0.5f ? ColorUtil.withAlpha(ThemeManager.accent(0.33f), 230) : track);
        float r = h / 2f - 1.5f;
        float kx = x + h / 2f + (w - h) * anim;
        Render2D.circle(ctx, kx, y + h / 2f, r + (hover ? 0.4f : 0f), 0xFFFFFFFF);
    }

    public static void button(DrawContext ctx, float x, float y, float w, float h, String label, boolean hover, boolean primary) {
        Theme t = t();
        if (primary) {
            Render2D.glow(ctx, x, y, w, h, 4, hover ? 1f : 0.5f, ThemeManager.accent(0));
            Render2D.roundRectH(ctx, x, y, w, h, 4, ThemeManager.accent(0), ThemeManager.accent(0.33f));
        } else {
            Render2D.roundRect(ctx, x, y, w, h, 4, hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surfaceHover(), 160));
            Render2D.roundRectOutline(ctx, x, y, w, h, 4, 0.8f, hover ? ThemeManager.accent(0) : t.stroke());
        }
        float tw = width(label);
        text(ctx, label, x + (w - tw) / 2f, y + (h - 8) / 2f, primary ? 0xFFFFFFFF : t.text());
    }

    public static void chip(DrawContext ctx, float x, float y, float w, float h, String label, boolean active, boolean hover) {
        Theme t = t();
        if (active) {
            Render2D.roundRectH(ctx, x, y, w, h, h / 2f, ColorUtil.withAlpha(ThemeManager.accent(0), 210), ColorUtil.withAlpha(ThemeManager.accent(0.33f), 210));
        } else {
            Render2D.roundRect(ctx, x, y, w, h, h / 2f, hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surfaceHover(), 140));
            Render2D.roundRectOutline(ctx, x, y, w, h, h / 2f, 0.7f, t.stroke());
        }
        float tw = smallWidth(label);
        small(ctx, label, x + (w - tw) / 2f, y + (h - 6) / 2f, active ? 0xFFFFFFFF : t.text());
    }

    public static float chipWidth(String label) { return smallWidth(label) + 12; }

    public static void tooltip(DrawContext ctx, String text, float mx, float my, float screenW, float screenH) {
        if (text == null || text.isEmpty()) return;
        Theme t = t();
        java.util.List<String> lines = wrap(text, 180);
        float w = 0;
        for (String l : lines) w = Math.max(w, smallWidth(l));
        float h = lines.size() * 8 + 6;
        float x = Math.min(mx + 10, screenW - w - 12), y = Math.min(my + 10, screenH - h - 4);
        Render2D.shadow(ctx, x, y, w + 8, h, 4, 6, 0x80000000);
        Render2D.roundRect(ctx, x, y, w + 8, h, 4, ColorUtil.withAlpha(t.background(), 245));
        Render2D.roundRectOutline(ctx, x, y, w + 8, h, 4, 0.8f, ThemeManager.accent(0));
        float ly = y + 4;
        for (String l : lines) {
            small(ctx, l, x + 4, ly, t.text());
            ly += 8;
        }
    }

    public static java.util.List<String> wrap(String text, float maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String cand = line.isEmpty() ? word : line + " " + word;
            if (smallWidth(cand) > maxWidth && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(cand);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    /** Small monochrome icon from textures/icons (tinted). */
    public static void icon(DrawContext ctx, String name, float x, float y, float size, int color) {
        Render2D.texture(ctx, net.minecraft.util.Identifier.of("voidclient", "textures/icons/" + name + ".png"), x, y, size, size, color);
    }
}
