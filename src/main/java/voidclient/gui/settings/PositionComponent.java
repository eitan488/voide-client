package voidclient.gui.settings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.PositionSetting;
import voidclient.gui.components.UI;
import voidclient.gui.screens.HudEditorScreen;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** X/Y percentage sliders plus a shortcut into the HUD editor. */
public class PositionComponent extends SettingComponent<PositionSetting> {
    private int dragging;

    public PositionComponent(PositionSetting s) { super(s); }

    @Override
    public float height() { return 44; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        if (dragging != 0) apply(mx);
        label(ctx, y + 2);
        String btn = "HUD Editor";
        float bw = UI.width(btn) + 10;
        UI.button(ctx, x + w - bw, y, bw, 12, btn, mx >= x + w - bw && mx < x + w && my >= y && my < y + 12, false);
        slider(ctx, y + 18, "X", setting.x());
        slider(ctx, y + 32, "Y", setting.y());
    }

    private void slider(DrawContext ctx, float yy, String axis, double v) {
        UI.small(ctx, axis, x, yy - 1, UI.t().textDim());
        String pct = String.format(Locale.ROOT, "%.1f%%", v * 100);
        UI.small(ctx, pct, x + w - UI.smallWidth(pct), yy - 1, ThemeManager.accent(0));
        float sx = x + 10, sw = w - 46;
        Render2D.roundRect(ctx, sx, yy + 1, sw, 3, 1.5f, ColorUtil.withAlpha(UI.t().textDim(), 60));
        Render2D.roundRectH(ctx, sx, yy + 1, (float) (sw * v), 3, 1.5f, ThemeManager.accent(0), ThemeManager.accent(0.33f));
        Render2D.circle(ctx, sx + (float) (sw * v), yy + 2.5f, 3f, 0xFFFFFFFF);
    }

    private void apply(double mx) {
        float sx = x + 10, sw = w - 46;
        double f = Math.max(0, Math.min(1, (mx - sx) / sw));
        if (dragging == 1) setting.set(new PositionSetting.Pos(f, setting.y()));
        else setting.set(new PositionSetting.Pos(setting.x(), f));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        String btn = "HUD Editor";
        float bw = UI.width(btn) + 10;
        if (my < y + 12 && mx >= x + w - bw) {
            ModuleManager.playClick();
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new HudEditorScreen(mc.currentScreen));
            return true;
        }
        if (button == 1) {
            setting.reset();
            return true;
        }
        if (my >= y + 14 && my < y + 26) dragging = 1;
        else if (my >= y + 28) dragging = 2;
        if (dragging != 0) apply(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging != 0;
        dragging = 0;
        return was;
    }
}
