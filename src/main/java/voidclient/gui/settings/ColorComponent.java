package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.ColorSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Swatch that expands into an HSV picker with alpha, hex input, chroma and theme binding. */
public class ColorComponent extends SettingComponent<ColorSetting> {
    private boolean open;
    private final Animation expand = new Animation(0, 220, Easing.CUBIC_OUT);
    private final TextField hex = new TextField("#AARRGGBB", 9);
    private int drag; // 0 none, 1 sv, 2 hue, 3 alpha
    private float hue, sat, bri;
    private static final float PICKER = 92;

    public ColorComponent(ColorSetting s) {
        super(s);
        syncFromSetting();
        hex.onSubmit(text -> {
            int c = ColorUtil.parseHex(text, setting.raw());
            setting.setArgb(c);
            syncFromSetting();
        });
    }

    private void syncFromSetting() {
        float[] hsb = ColorUtil.toHsb(setting.raw());
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        hex.setText(ColorUtil.toHex(setting.raw(), true));
    }

    @Override
    public float height() { return 18 + (float) expand.update() * PICKER; }

    @Override
    public boolean capturesKeyboard() { return hex.capturesKeyboard(); }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = UI.t();
        expand.target(open ? 1 : 0);
        if (drag != 0) applyDrag(mx, my);
        label(ctx, y + 5);
        int c = setting.getColor();
        float sx = x + w - 30;
        Render2D.roundRect(ctx, sx, y + 2.5f, 28, 13, 3, 0xFF2A2A2A);
        Render2D.roundRect(ctx, sx, y + 2.5f, 28, 13, 3, c);
        Render2D.roundRectOutline(ctx, sx, y + 2.5f, 28, 13, 3, 0.8f, open ? ThemeManager.accent(0) : t.stroke());
        String tag = setting.get().themed() ? "THEME" : setting.get().chroma() ? "CHROMA" : null;
        if (tag != null) UI.small(ctx, tag, sx - UI.smallWidth(tag) - 4, y + 6.5f, ThemeManager.accent(0));
        float e = (float) expand.update();
        if (e <= 0.01f) return;
        Render2D.scissor(ctx, x, y + 18, w, PICKER * e);
        float py = y + 20;
        float svW = w - 34, svH = 60;
        int pure = ColorUtil.hsb(hue, 1, 1);
        Render2D.gradientH(ctx, x, py, svW, svH, 0xFFFFFFFF, pure);
        Render2D.gradientV(ctx, x, py, svW, svH, 0x00000000, 0xFF000000);
        Render2D.outline(ctx, x, py, svW, svH, 0.6f, t.stroke());
        Render2D.circle(ctx, x + sat * svW, py + (1 - bri) * svH, 3f, 0xFFFFFFFF);
        Render2D.circle(ctx, x + sat * svW, py + (1 - bri) * svH, 2f, ColorUtil.hsb(hue, sat, bri));
        float hx = x + svW + 5;
        for (int i = 0; i < 6; i++) {
            Render2D.gradientV(ctx, hx, py + svH * i / 6f, 10, svH / 6f + 0.3f, ColorUtil.hsb(i / 6f, 1, 1), ColorUtil.hsb((i + 1) / 6f, 1, 1));
        }
        Render2D.rect(ctx, hx - 1, py + hue * svH - 0.75f, 12, 1.5f, 0xFFFFFFFF);
        float ax = hx + 14;
        if (setting.allowsAlpha()) {
            int opaque = setting.raw() | 0xFF000000;
            Render2D.gradientV(ctx, ax, py, 10, svH, opaque, opaque & 0x00FFFFFF);
            Render2D.outline(ctx, ax, py, 10, svH, 0.6f, t.stroke());
            float a = ColorUtil.alpha(setting.raw()) / 255f;
            Render2D.rect(ctx, ax - 1, py + (1 - a) * svH - 0.75f, 12, 1.5f, 0xFFFFFFFF);
        }
        float by = py + svH + 5;
        hex.bounds(x, by, w * 0.42f, 14);
        hex.render(ctx, mx, my, delta);
        float cx = x + w * 0.42f + 4;
        float cw = (w - (cx - x) - 4) / 2f;
        UI.chip(ctx, cx, by + 1, cw, 12, "Chroma", setting.get().chroma(), mx >= cx && mx < cx + cw && my >= by && my < by + 14);
        UI.chip(ctx, cx + cw + 4, by + 1, cw, 12, "Theme", setting.get().themed(), mx >= cx + cw + 4 && mx < x + w && my >= by && my < by + 14);
        Render2D.endScissor(ctx);
    }

    private void applyDrag(double mx, double my) {
        float py = y + 20, svW = w - 34, svH = 60;
        switch (drag) {
            case 1 -> {
                sat = (float) Math.max(0, Math.min(1, (mx - x) / svW));
                bri = 1f - (float) Math.max(0, Math.min(1, (my - py) / svH));
            }
            case 2 -> hue = (float) Math.max(0, Math.min(0.999, (my - py) / svH));
            case 3 -> {
                float a = 1f - (float) Math.max(0, Math.min(1, (my - py) / svH));
                setting.setArgb(ColorUtil.withAlpha(setting.raw(), Math.round(a * 255)));
                hex.setText(ColorUtil.toHex(setting.raw(), true));
                return;
            }
            default -> {
                return;
            }
        }
        int rgb = ColorUtil.hsb(hue, sat, bri);
        setting.setArgb(ColorUtil.withAlpha(rgb, ColorUtil.alpha(setting.raw())));
        hex.setText(ColorUtil.toHex(setting.raw(), true));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) {
            hex.setFocused(false);
            return false;
        }
        if (my < y + 18) {
            if (button == 1) {
                setting.reset();
                syncFromSetting();
            } else {
                open = !open;
                if (open) syncFromSetting();
            }
            ModuleManager.playClick();
            return true;
        }
        if (!open) return false;
        float py = y + 20, svW = w - 34, svH = 60;
        if (my >= py && my < py + svH) {
            if (mx < x + svW) drag = 1;
            else if (mx >= x + svW + 5 && mx < x + svW + 15) drag = 2;
            else if (setting.allowsAlpha() && mx >= x + svW + 19 && mx < x + svW + 29) drag = 3;
            if (drag != 0) {
                if (setting.get().chroma()) setting.setChroma(false);
                if (setting.get().themed()) setting.setThemed(false);
                applyDrag(mx, my);
            }
            return true;
        }
        float by = py + svH + 5;
        if (hex.mouseClicked(mx, my, button)) return true;
        float cx = x + w * 0.42f + 4, cw = (w - (cx - x) - 4) / 2f;
        if (my >= by && my < by + 14) {
            if (mx >= cx && mx < cx + cw) setting.setChroma(!setting.get().chroma());
            else if (mx >= cx + cw + 4) setting.setThemed(!setting.get().themed());
            ModuleManager.playClick();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = drag != 0;
        drag = 0;
        return was;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) { return hex.keyPressed(key, scancode, mods); }

    @Override
    public boolean charTyped(String chars) { return hex.charTyped(chars); }
}
