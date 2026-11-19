package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.Setting;
import voidclient.gui.components.UI;
import voidclient.gui.components.Widget;
import voidclient.gui.themes.Theme;

/** A row in the settings panel bound to one {@link Setting}. */
public abstract class SettingComponent<S extends Setting<?>> extends Widget {
    protected final S setting;

    protected SettingComponent(S setting) { this.setting = setting; }

    public S setting() { return setting; }

    public boolean isVisibleSetting() { return setting.isVisible(); }

    /** Height for the current state (dropdowns and pickers grow when open). */
    public abstract float height();

    /** Lay out at (x, y) with width w; height comes from {@link #height()}. */
    public void layout(float x, float y, float w) {
        bounds(x, y, w, height());
    }

    protected void label(DrawContext ctx, float yy) {
        Theme t = UI.t();
        UI.text(ctx, setting.getName(), x, yy, setting.isDefault() ? t.text() : 0xFFFFFFFF);
        if (!setting.isDefault()) {
            float lw = UI.width(setting.getName());
            voidclient.render.Render2D.circle(ctx, x + lw + 4, yy + 4, 1.5f, voidclient.gui.themes.ThemeManager.accent(0));
        }
    }

    /** Right click on the label row resets the setting. */
    protected boolean resetOnRightClick(double mx, double my, int button, float rowHeight) {
        if (button == 1 && mx >= x && mx < x + w && my >= y && my < y + rowHeight) {
            setting.reset();
            return true;
        }
        return false;
    }

    public String tooltip() { return setting.getDescription() + "  (right-click to reset)"; }
}
