package voidclient.gui;

import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Animated toast notifications (bottom-right stack). */
public final class Notifications {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private static final class Toast {
        final String title, body;
        final Type type;
        final long created = System.currentTimeMillis();
        final long duration;
        final Animation slide = new Animation(0, 320, Easing.BACK_OUT);
        final Animation y = new Animation(-1, 220, Easing.CUBIC_OUT);
        boolean leaving;

        Toast(String title, String body, Type type, long duration) {
            this.title = title;
            this.body = body;
            this.type = type;
            this.duration = duration;
            slide.target(1);
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final List<String> HISTORY = new ArrayList<>();
    private static final int MAX = 6;

    private Notifications() {}

    public static void info(String title, String body) { push(title, body, Type.INFO); }
    public static void success(String title, String body) { push(title, body, Type.SUCCESS); }
    public static void warning(String title, String body) { push(title, body, Type.WARNING); }
    public static void error(String title, String body) { push(title, body, Type.ERROR); }

    public static synchronized void push(String title, String body, Type type) {
        var system = VoidClient.get() == null ? null : VoidClient.get().notificationSettings();
        if (system != null && !system.accepts(type)) return;
        long duration = system != null ? system.durationMs() : 2500;
        TOASTS.add(0, new Toast(title, body, type, duration));
        while (TOASTS.size() > MAX) TOASTS.remove(TOASTS.size() - 1);
        HISTORY.add(0, "[" + type.name() + "] " + title + (body == null || body.isEmpty() ? "" : " - " + body));
        while (HISTORY.size() > 50) HISTORY.remove(HISTORY.size() - 1);
        if (system != null) system.playSound(type);
    }

    public static synchronized List<String> history() { return List.copyOf(HISTORY); }

    private static int color(Type t) {
        Theme th = ThemeManager.get();
        return switch (t) {
            case INFO -> ThemeManager.accent(0);
            case SUCCESS -> th.success();
            case WARNING -> th.warning();
            case ERROR -> th.danger();
        };
    }

    public static synchronized void render(DrawContext ctx, int sw, int sh) {
        if (TOASTS.isEmpty()) return;
        var system = VoidClient.get().notificationSettings();
        float scale = system != null ? system.scale() : 1f;
        boolean top = system != null && system.topCorner();
        Theme th = ThemeManager.get();
        long now = System.currentTimeMillis();
        float w = 150, h = 30, gap = 4;
        float stack = 0;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(scale, scale);
        float vw = sw / scale, vh = sh / scale;
        int index = 0;
        for (Iterator<Toast> it = TOASTS.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            if (!t.leaving && now - t.created > t.duration) {
                t.leaving = true;
                t.slide.target(0);
            }
            float s = t.slide.get();
            if (t.leaving && s <= 0.01f) {
                it.remove();
                continue;
            }
            t.y.target(stack);
            float yOff = t.y.target() < 0 ? stack : t.y.get();
            if (t.y.target() >= 0 && yOff < 0) yOff = stack;
            float x = vw - (w + 8) * Math.min(1f, s);
            float y = top ? 8 + yOff : vh - 8 - h - yOff;
            int c = color(t.type);
            float a = Math.min(1f, s);
            Render2D.shadow(ctx, x, y, w, h, 5, 8, ColorUtil.multiplyAlpha(0x70000000, a));
            Render2D.roundRect(ctx, x, y, w, h, 5, ColorUtil.multiplyAlpha(th.surface(), a));
            Render2D.roundRect(ctx, x, y, 3, h, 1.5f, ColorUtil.multiplyAlpha(c, a));
            float progress = 1f - Math.min(1f, (now - t.created) / (float) t.duration);
            Render2D.rect(ctx, x + 4, y + h - 2, (w - 8) * progress, 1, ColorUtil.multiplyAlpha(c, a * 0.8f));
            FontRenderer.draw(ctx, FontRenderer.trim(t.title, w - 14, FontRenderer.Family.INTER_BOLD), x + 9, y + 5, ColorUtil.multiplyAlpha(th.text(), a), FontRenderer.Family.INTER_BOLD, false);
            if (t.body != null && !t.body.isEmpty()) {
                FontRenderer.draw(ctx, FontRenderer.trim(t.body, w - 14, FontRenderer.Family.INTER), x + 9, y + 16, ColorUtil.multiplyAlpha(th.textDim(), a), FontRenderer.Family.INTER, false);
            }
            stack += (h + gap) * Math.min(1f, s);
            index++;
        }
        m.popMatrix();
    }
}
