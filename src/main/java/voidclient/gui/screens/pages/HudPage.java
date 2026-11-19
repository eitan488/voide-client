package voidclient.gui.screens.pages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.HudModule;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.UI;
import voidclient.gui.screens.ClickGuiScreen;
import voidclient.gui.screens.HudEditorScreen;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.List;

/** HUD overview: open the editor, toggle visibility and layers of every HUD element. */
public class HudPage extends Page {
    private final ClickGuiScreen owner;
    private float scroll;

    public HudPage(ClickGuiScreen owner) { this.owner = owner; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        UI.bold(ctx, "HUD Editor", x + 2, y + 2, 0xFFFFFFFF);
        UI.small(ctx, "Drag, resize, align, snap and layer every HUD element.", x + 2, y + 14, t.textDim());
        UI.button(ctx, x, y + 26, 130, 18, "Open Full HUD Editor", in(mx, my, x, y + 26, 130, 18), true);
        UI.button(ctx, x + 136, y + 26, 110, 18, "Reset All Positions", in(mx, my, x + 136, y + 26, 110, 18), false);
        List<HudModule> huds = VoidClient.get().hud().sortedElements();
        float ly = y + 52, rowH = 18;
        Render2D.scissor(ctx, x, ly, w, h - (ly - y));
        float ry = ly - scroll;
        UI.small(ctx, "ELEMENT", x + 6, ry, t.textDim());
        UI.small(ctx, "LAYER", x + w - 150, ry, t.textDim());
        UI.small(ctx, "SCALE", x + w - 100, ry, t.textDim());
        UI.small(ctx, "VISIBLE", x + w - 40, ry, t.textDim());
        ry += 10;
        for (HudModule hm : huds) {
            boolean hover = in(mx, my, x, ry, w, rowH - 2);
            Render2D.roundRect(ctx, x, ry, w, rowH - 2, 4, hover ? t.surfaceHover() : ThemeManager.panel(t.surface()));
            UI.text(ctx, hm.getName(), x + 6, ry + 4, hm.isEnabled() ? t.text() : t.textDim());
            UI.small(ctx, hm.getCategory().displayName, x + 10 + UI.width(hm.getName()), ry + 5, ColorUtil.withAlpha(ThemeManager.accent(0), 160));
            UI.button(ctx, x + w - 156, ry + 2, 12, 12, "-", in(mx, my, x + w - 156, ry + 2, 12, 12), false);
            UI.text(ctx, String.valueOf(hm.layer.getInt()), x + w - 140, ry + 4, t.text());
            UI.button(ctx, x + w - 128, ry + 2, 12, 12, "+", in(mx, my, x + w - 128, ry + 2, 12, 12), false);
            UI.text(ctx, hm.scale.format(), x + w - 100, ry + 4, t.text());
            UI.toggle(ctx, x + w - 36, ry + 3, 20, 10, hm.isEnabled() ? 1 : 0, false);
            ry += rowH;
        }
        Render2D.endScissor(ctx);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, (ry + scroll) - (y + h))));
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (in(mx, my, x, y + 26, 130, 18)) {
            mc.setScreen(new HudEditorScreen(owner));
            ModuleManager.playClick();
            return true;
        }
        if (in(mx, my, x + 136, y + 26, 110, 18)) {
            for (HudModule hm : VoidClient.get().modules().hudModules()) {
                hm.position.reset();
                hm.scale.reset();
                hm.layer.reset();
            }
            Notifications.info("HUD", "Positions reset");
            ModuleManager.playClick();
            return true;
        }
        float ry = y + 52 - scroll + 10, rowH = 18;
        for (HudModule hm : VoidClient.get().hud().sortedElements()) {
            if (my >= ry && my < ry + rowH - 2) {
                if (in(mx, my, x + w - 156, ry + 2, 12, 12)) hm.layer.set(hm.layer.get() - 1);
                else if (in(mx, my, x + w - 128, ry + 2, 12, 12)) hm.layer.set(hm.layer.get() + 1);
                else if (mx >= x + w - 40) VoidClient.get().modules().userToggle(hm, false);
                else return true;
                ModuleManager.playClick();
                return true;
            }
            ry += rowH;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll -= (float) amount * 20;
        if (scroll < 0) scroll = 0;
        return true;
    }
}
