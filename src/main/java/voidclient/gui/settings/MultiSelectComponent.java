package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.gui.components.UI;

/** Flow layout of toggle chips. */
public class MultiSelectComponent extends SettingComponent<MultiSelectSetting> {
    private float lastW = 200;

    public MultiSelectComponent(MultiSelectSetting s) { super(s); }

    private float layoutChips(float width, DrawContext ctx, double mx, double my) {
        float cx = 0, cy = 14;
        for (String o : setting.getOptions()) {
            float cw = UI.chipWidth(o);
            if (cx + cw > width && cx > 0) {
                cx = 0;
                cy += 14;
            }
            if (ctx != null) {
                float px = x + cx, py = y + cy;
                UI.chip(ctx, px, py, cw, 12, o, setting.isSelected(o), mx >= px && mx < px + cw && my >= py && my < py + 12);
            }
            cx += cw + 3;
        }
        return cy + 14;
    }

    @Override
    public float height() { return layoutChips(lastW, null, 0, 0) + 2; }

    @Override
    public void layout(float x, float y, float w) {
        lastW = w;
        super.layout(x, y, w);
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        label(ctx, y + 2);
        String count = setting.get().size() + "/" + setting.getOptions().size();
        UI.small(ctx, count, x + w - UI.smallWidth(count), y + 3, UI.t().textDim());
        layoutChips(w, ctx, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (resetOnRightClick(mx, my, button, 12)) return true;
        float cx = 0, cy = 14;
        for (String o : setting.getOptions()) {
            float cw = UI.chipWidth(o);
            if (cx + cw > w && cx > 0) {
                cx = 0;
                cy += 14;
            }
            float px = x + cx, py = y + cy;
            if (mx >= px && mx < px + cw && my >= py && my < py + 12) {
                setting.toggle(o);
                ModuleManager.playClick();
                return true;
            }
            cx += cw + 3;
        }
        return true;
    }
}
