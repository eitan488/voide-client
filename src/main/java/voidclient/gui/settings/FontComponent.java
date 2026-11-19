package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.FontSetting;
import voidclient.gui.components.UI;
import voidclient.render.FontRenderer;

/** Font family chips, shadow toggle and a live preview in the chosen font. */
public class FontComponent extends SettingComponent<FontSetting> {
    public FontComponent(FontSetting s) { super(s); }

    @Override
    public float height() { return 44; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        label(ctx, y + 2);
        FontRenderer.Family[] fams = FontRenderer.Family.values();
        float cw = (w - 3 * (fams.length - 1)) / fams.length;
        for (int i = 0; i < fams.length; i++) {
            float cx = x + i * (cw + 3);
            UI.chip(ctx, cx, y + 13, cw, 12, fams[i].displayName, setting.family() == fams[i], mx >= cx && mx < cx + cw && my >= y + 13 && my < y + 25);
        }
        UI.chip(ctx, x + w - 44, y, 44, 11, "Shadow", setting.shadow(), mx >= x + w - 44 && mx < x + w && my >= y && my < y + 11);
        FontRenderer.draw(ctx, "The void stares back 0123", x, y + 31, UI.t().text(), setting.family(), setting.shadow());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (resetOnRightClick(mx, my, button, 11)) return true;
        if (my < y + 11 && mx >= x + w - 44) {
            setting.setShadow(!setting.shadow());
            ModuleManager.playClick();
            return true;
        }
        FontRenderer.Family[] fams = FontRenderer.Family.values();
        float cw = (w - 3 * (fams.length - 1)) / fams.length;
        if (my >= y + 13 && my < y + 25) {
            int i = (int) ((mx - x) / (cw + 3));
            if (i >= 0 && i < fams.length) {
                setting.setFamily(fams[i]);
                ModuleManager.playClick();
            }
        }
        return true;
    }
}
