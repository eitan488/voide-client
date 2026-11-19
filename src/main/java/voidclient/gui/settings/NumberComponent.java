package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.components.UI;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

public class NumberComponent extends SettingComponent<NumberSetting> {
    private boolean dragging;
    private final Animation fill;

    public NumberComponent(NumberSetting s) {
        super(s);
        fill = new Animation(fraction(), 120);
    }

    private double fraction() { return (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin()); }

    @Override
    public float height() { return 26; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        if (dragging) setFrom(mx);
        fill.target(fraction());
        label(ctx, y + 3);
        String v = setting.format();
        UI.text(ctx, v, x + w - UI.width(v) - 1, y + 3, ThemeManager.accent(0));
        float ty = y + 17, th = 4;
        float f = (float) Math.max(0, Math.min(1, fill.update()));
        Render2D.roundRect(ctx, x, ty, w, th, 2, ColorUtil.withAlpha(UI.t().textDim(), 60));
        if (f > 0.001f) Render2D.roundRectH(ctx, x, ty, w * f, th, 2, ThemeManager.accent(0), ThemeManager.accent(0.33f));
        boolean hover = hovered(mx, my) || dragging;
        float kx = x + w * f;
        if (hover) Render2D.circle(ctx, kx, ty + th / 2f, 5f, ColorUtil.withAlpha(ThemeManager.accent(0), 60));
        Render2D.circle(ctx, kx, ty + th / 2f, hover ? 3.8f : 3.2f, 0xFFFFFFFF);
    }

    private void setFrom(double mx) {
        double f = Math.max(0, Math.min(1, (mx - x) / w));
        setting.set(setting.getMin() + f * (setting.getMax() - setting.getMin()));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (button == 1) {
            setting.reset();
            return true;
        }
        dragging = true;
        setFrom(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!hovered(mx, my)) return false;
        setting.set(setting.get() + Math.signum(amount) * setting.getStep());
        return true;
    }
}
