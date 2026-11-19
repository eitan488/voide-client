package voidclient.gui.screens.pages;

import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.Module;
import voidclient.core.ModuleManager;
import voidclient.core.setting.Setting;
import voidclient.gui.Notifications;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.settings.KeybindComponent;
import voidclient.gui.settings.SettingsPanel;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.modules.visuals.ThemeEngine;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Client settings, theme engine and the keybind manager. */
public class SettingsPage extends Page {
    private enum Tab { GENERAL, THEME, KEYBINDS }

    private static Tab tab = Tab.GENERAL;
    private SettingsPanel general, theme;
    private final TextField search = new TextField("Search keybinds...", 32);
    private final List<KeybindComponent> binds = new ArrayList<>();
    private final List<Module> bindModules = new ArrayList<>();
    private float bindScroll;
    private String tip;

    public SettingsPage() {
        general = new SettingsPanel(VoidClient.get().clientSettings().all(), null);
        List<Setting<?>> themeSettings = new ArrayList<>(VoidClient.get().modules().get(ThemeEngine.class).getSettings());
        themeSettings.removeIf(s -> s.getName().equals("Keybind") || s.getName().equals("Toggle Notification") || s.getName().equals("Show In Module List"));
        theme = new SettingsPanel(themeSettings, null);
        search.onChange(s -> rebuildBinds());
        rebuildBinds();
    }

    private void rebuildBinds() {
        binds.clear();
        bindModules.clear();
        String q = search.getText().toLowerCase(Locale.ROOT);
        for (Module m : VoidClient.get().modules().all()) {
            if (!q.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(q)) continue;
            binds.add(new KeybindComponent(m.keybind, m));
            bindModules.add(m);
        }
    }

    @Override
    public String tooltip() { return tip; }

    @Override
    public boolean capturesKeyboard() {
        if (search.capturesKeyboard()) return true;
        if (tab == Tab.KEYBINDS) for (KeybindComponent k : binds) if (k.capturesKeyboard()) return true;
        if (tab == Tab.GENERAL) return general.capturesKeyboard();
        if (tab == Tab.THEME) return theme.capturesKeyboard();
        return false;
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        tip = null;
        Theme t = ThemeManager.get();
        float tx = x;
        for (Tab tb : Tab.values()) {
            String label = tb.name().charAt(0) + tb.name().substring(1).toLowerCase(Locale.ROOT);
            float cw = UI.chipWidth(label) + 10;
            UI.chip(ctx, tx, y, cw, 14, label, tab == tb, mx >= tx && mx < tx + cw && my >= y && my < y + 14);
            tx += cw + 4;
        }
        float cy = y + 20, ch = h - 20;
        switch (tab) {
            case GENERAL -> {
                general.bounds(x, cy, Math.min(w, 340), ch);
                general.render(ctx, mx, my, delta);
                if (general.hoveredComponent() != null) tip = general.hoveredComponent().tooltip();
                renderInfo(ctx, x + Math.min(w, 340) + 10, cy, w - Math.min(w, 340) - 10);
            }
            case THEME -> {
                theme.bounds(x, cy, Math.min(w, 340), ch);
                theme.render(ctx, mx, my, delta);
                if (theme.hoveredComponent() != null) tip = theme.hoveredComponent().tooltip();
                renderThemePreview(ctx, x + Math.min(w, 340) + 10, cy, w - Math.min(w, 340) - 10);
            }
            case KEYBINDS -> renderBinds(ctx, mx, my, delta, cy, ch, t);
        }
    }

    private void renderInfo(DrawContext ctx, float px, float py, float pw) {
        if (pw < 80) return;
        Theme t = ThemeManager.get();
        Render2D.roundRect(ctx, px, py, pw, 110, 6, ThemeManager.panel(t.surface()));
        UI.bold(ctx, "Void Client " + VoidClient.VERSION, px + 8, py + 8, 0xFFFFFFFF);
        String[] lines = {
                "Minecraft Java " + VoidClient.MC_VERSION + " • Fabric",
                VoidClient.get().modules().all().size() + " modules registered",
                "Open GUI: " + VoidClient.get().openGuiKey().getBoundKeyLocalizedText().getString(),
                "Config: config/voidclient/",
                "Right-click any setting to reset it.",
                "Middle-click a card to favourite it."
        };
        float ly = py + 22;
        for (String l : lines) {
            UI.small(ctx, l, px + 8, ly, t.textDim());
            ly += 10;
        }
    }

    private void renderThemePreview(DrawContext ctx, float px, float py, float pw) {
        if (pw < 80) return;
        Theme t = ThemeManager.get();
        Render2D.glow(ctx, px, py, pw, 120, 8, ThemeManager.glowStrength(), ThemeManager.accent(0));
        Render2D.roundRect(ctx, px, py, pw, 120, 8, ThemeManager.panel(t.background()));
        Render2D.roundRectOutlineGradient(ctx, px, py, pw, 120, 8, 1f, ThemeManager.accent(0), ThemeManager.accent(0.5f));
        UI.bold(ctx, t.name() + " theme", px + 10, py + 10, 0xFFFFFFFF);
        int[] swatches = {t.background(), t.surface(), t.accent1(), t.accent2(), t.accent3(), t.text(), t.success(), t.warning(), t.danger()};
        float sw = (pw - 20 - 8 * 3) / 9f;
        for (int i = 0; i < swatches.length; i++) {
            Render2D.roundRect(ctx, px + 10 + i * (sw + 3), py + 26, sw, 16, 3, swatches[i] | 0xFF000000);
        }
        UI.button(ctx, px + 10, py + 50, pw / 2f - 14, 16, "Primary", false, true);
        UI.button(ctx, px + pw / 2f + 4, py + 50, pw / 2f - 14, 16, "Secondary", false, false);
        UI.toggle(ctx, px + 10, py + 76, 22, 11, 1, false);
        UI.toggle(ctx, px + 40, py + 76, 22, 11, 0, false);
        Render2D.roundRectH(ctx, px + 10, py + 98, pw - 20, 4, 2, ThemeManager.accent(0), ThemeManager.accent(0.33f));
    }

    private void renderBinds(DrawContext ctx, double mx, double my, float delta, float cy, float ch, Theme t) {
        search.bounds(x, cy, 160, 15);
        search.render(ctx, mx, my, delta);
        UI.button(ctx, x + 166, cy, 70, 15, "Clear All", mx >= x + 166 && mx < x + 236 && my >= cy && my < cy + 15, false);
        UI.button(ctx, x + 240, cy, 90, 15, "Reset Defaults", mx >= x + 240 && mx < x + 330 && my >= cy && my < cy + 15, false);
        float ly = cy + 20, lh = ch - 20;
        Render2D.scissor(ctx, x, ly, w, lh);
        float ry = ly - bindScroll;
        float colW = (w - 10) / 2f;
        for (int i = 0; i < binds.size(); i++) {
            KeybindComponent k = binds.get(i);
            Module m = bindModules.get(i);
            float bx = x + (i % 2) * (colW + 10);
            float rowY = ry + (i / 2) * 32f;
            if (rowY + 30 < ly || rowY > ly + lh) continue;
            Render2D.roundRect(ctx, bx, rowY, colW, 30, 5, ThemeManager.panel(t.surface()));
            UI.small(ctx, m.getCategory().displayName.toUpperCase(Locale.ROOT), bx + 6, rowY + 3, ColorUtil.withAlpha(ThemeManager.accent(0), 170));
            k.layout(bx + 6, rowY + 9, colW - 12);
            boolean inside = my >= ly && my < ly + lh;
            k.render(ctx, inside ? mx : -9999, inside ? my : -9999, delta);
            if (inside && k.hovered(mx, my)) tip = m.getName() + ": " + m.getDescription();
        }
        Render2D.endScissor(ctx);
        float content = ((binds.size() + 1) / 2) * 32f;
        bindScroll = Math.max(0, Math.min(bindScroll, Math.max(0, content - lh)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float tx = x;
        for (Tab tb : Tab.values()) {
            String label = tb.name().charAt(0) + tb.name().substring(1).toLowerCase(Locale.ROOT);
            float cw = UI.chipWidth(label) + 10;
            if (mx >= tx && mx < tx + cw && my >= y && my < y + 14) {
                tab = tb;
                ModuleManager.playClick();
                return true;
            }
            tx += cw + 4;
        }
        float cy = y + 20;
        switch (tab) {
            case GENERAL -> {
                return general.mouseClicked(mx, my, button);
            }
            case THEME -> {
                return theme.mouseClicked(mx, my, button);
            }
            case KEYBINDS -> {
                for (KeybindComponent k : binds) {
                    if (k.isListening()) return k.mouseClicked(mx, my, button);
                }
                if (search.mouseClicked(mx, my, button)) return true;
                if (my >= cy && my < cy + 15) {
                    if (mx >= x + 166 && mx < x + 236) {
                        VoidClient.get().keybinds().clearAll();
                        Notifications.info("Keybinds", "All module keybinds cleared");
                        return true;
                    }
                    if (mx >= x + 240 && mx < x + 330) {
                        for (Module m : VoidClient.get().modules().all()) m.keybind.reset();
                        Notifications.info("Keybinds", "Defaults restored");
                        return true;
                    }
                }
                float ly = cy + 20;
                if (my < ly) return false;
                for (KeybindComponent k : binds) if (k.mouseClicked(mx, my, button)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return tab == Tab.GENERAL ? general.mouseReleased(mx, my, button) : tab == Tab.THEME && theme.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (tab == Tab.GENERAL) return general.mouseScrolled(mx, my, amount);
        if (tab == Tab.THEME) return theme.mouseScrolled(mx, my, amount);
        bindScroll -= (float) amount * 24;
        if (bindScroll < 0) bindScroll = 0;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (search.capturesKeyboard()) return search.keyPressed(key, scancode, mods);
        if (tab == Tab.KEYBINDS) {
            for (KeybindComponent k : binds) if (k.capturesKeyboard()) return k.keyPressed(key, scancode, mods);
        }
        if (tab == Tab.GENERAL) return general.keyPressed(key, scancode, mods);
        if (tab == Tab.THEME) return theme.keyPressed(key, scancode, mods);
        return false;
    }

    @Override
    public boolean charTyped(String chars) {
        if (search.capturesKeyboard()) return search.charTyped(chars);
        if (tab == Tab.GENERAL) return general.charTyped(chars);
        if (tab == Tab.THEME) return theme.charTyped(chars);
        return false;
    }
}
