package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Dropdown with a smooth expand animation. */
public class ModeComponent extends SettingComponent<ModeSetting> {
    private boolean open;
    private final Animation expand = new Animation(0, 200, Easing.CUBIC_OUT);
    private static final float ROW = 13;

    public ModeComponent(ModeSetting s) { super(s); }

    @Override
    public float height() {
        return 18 + (float) expand.update() * (setting.getModes().size() * ROW + 4);
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = UI.t();
        expand.target(open ? 1 : 0);
        label(ctx, y + 5);
        String v = setting.get();
        float bw = Math.min(w * 0.55f, UI.width(v) + 20);
        float bx = x + w - bw;
        boolean hover = mx >= bx && mx < x + w && my >= y + 1 && my < y + 17;
        Render2D.roundRect(ctx, bx, y + 1.5f, bw, 15, 4, hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surfaceHover(), 160));
        Render2D.roundRectOutline(ctx, bx, y + 1.5f, bw, 15, 4, 0.7f, open ? ThemeManager.accent(0) : t.stroke());
        UI.text(ctx, UI.trim(v, bw - 16), bx + 5, y + 5, t.text());
        float cx = x + w - 7, cy = y + 9;
        float e = (float) expand.update();
        if (e < 0.5f) Render2D.triangle(ctx, cx - 2.5f, cy - 1.2f, cx + 2.5f, cy - 1.2f, cx, cy + 1.8f, t.textDim());
        else Render2D.triangle(ctx, cx - 2.5f, cy + 1.8f, cx, cy - 1.2f, cx + 2.5f, cy + 1.8f, ThemeManager.accent(0));
        if (e > 0.01f) {
            float listH = (setting.getModes().size() * ROW + 4) * e;
            Render2D.scissor(ctx, x, y + 18, w, listH);
            Render2D.roundRect(ctx, x, y + 18, w, setting.getModes().size() * ROW + 2, 4, ColorUtil.withAlpha(t.background(), 220));
            float ry = y + 19;
            for (String m : setting.getModes()) {
                boolean sel = m.equals(v);
                boolean h = mx >= x && mx < x + w && my >= ry && my < ry + ROW && my < y + 18 + listH;
                if (sel || h) Render2D.roundRect(ctx, x + 2, ry, w - 4, ROW - 1, 3, sel ? ColorUtil.withAlpha(ThemeManager.accent(0), 70) : t.surfaceHover());
                UI.text(ctx, m, x + 7, ry + 2.5f, sel ? 0xFFFFFFFF : t.textDim());
                ry += ROW;
            }
            Render2D.endScissor(ctx);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (my < y + 18) {
            if (button == 1) setting.reset();
            else if (button == 2) setting.cycle(true);
            else open = !open;
            ModuleManager.playClick();
            return true;
        }
        if (open) {
            int idx = (int) ((my - (y + 19)) / ROW);
            if (idx >= 0 && idx < setting.getModes().size()) {
                setting.set(setting.getModes().get(idx));
                open = false;
                ModuleManager.playClick();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!hovered(mx, my) || open) return false;
        setting.cycle(amount < 0);
        return true;
    }
}
