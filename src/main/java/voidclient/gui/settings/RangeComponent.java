package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.RangeSetting;
import voidclient.gui.components.UI;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Two-handle slider. */
public class RangeComponent extends SettingComponent<RangeSetting> {
    private int dragging; // 0 none, 1 low, 2 high

    public RangeComponent(RangeSetting s) { super(s); }

    @Override
    public float height() { return 26; }

    private double frac(double v) { return (v - setting.getMin()) / (setting.getMax() - setting.getMin()); }

    private String fmt(double v) {
        return setting.getStep() >= 1 ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        if (dragging != 0) apply(mx);
        label(ctx, y + 3);
        String v = fmt(setting.low()) + " - " + fmt(setting.high());
        UI.text(ctx, v, x + w - UI.width(v) - 1, y + 3, ThemeManager.accent(0));
        float ty = y + 17;
        float a = (float) frac(setting.low()), b = (float) frac(setting.high());
        Render2D.roundRect(ctx, x, ty, w, 4, 2, ColorUtil.withAlpha(UI.t().textDim(), 60));
        Render2D.roundRectH(ctx, x + w * a, ty, Math.max(1, w * (b - a)), 4, 2, ThemeManager.accent(0), ThemeManager.accent(0.33f));
        Render2D.circle(ctx, x + w * a, ty + 2, 3.4f, 0xFFFFFFFF);
        Render2D.circle(ctx, x + w * b, ty + 2, 3.4f, 0xFFFFFFFF);
    }

    private void apply(double mx) {
        double v = setting.getMin() + Math.max(0, Math.min(1, (mx - x) / w)) * (setting.getMax() - setting.getMin());
        if (dragging == 1) setting.set(new RangeSetting.Range(Math.min(v, setting.high()), setting.high()));
        else setting.set(new RangeSetting.Range(setting.low(), Math.max(v, setting.low())));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (button == 1) {
            setting.reset();
            return true;
        }
        double la = x + w * frac(setting.low()), lb = x + w * frac(setting.high());
        dragging = Math.abs(mx - la) <= Math.abs(mx - lb) ? 1 : 2;
        apply(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging != 0;
        dragging = 0;
        return was;
    }
}
