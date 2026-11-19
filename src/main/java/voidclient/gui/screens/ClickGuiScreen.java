package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.ModuleManager;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.screens.pages.DashboardPage;
import voidclient.gui.screens.pages.HudPage;
import voidclient.gui.screens.pages.Page;
import voidclient.gui.screens.pages.ProfilesPage;
import voidclient.gui.screens.pages.SettingsPage;
import voidclient.gui.settings.SettingsPanel;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The main Void Client window: sidebar, module cards, settings panel, top and bottom bars. */
public class ClickGuiScreen extends VoidScreen {
    private enum Filter { ALL, ENABLED, FAVORITES, RECENT }

    private static Category lastCategory = Category.DASHBOARD;
    private static float offsetX, offsetY;
    private static String lastSelected;

    private static final float TOP = 30, BOTTOM = 16, SIDEBAR = 132, RIGHT = 214;

    private Category category = lastCategory;
    private Filter filter = Filter.ALL;
    private final TextField search = new TextField("Search modules...", 40);
    private final Animation open = new Animation(0, 320, Easing.BACK_OUT);
    private final Animation fade = new Animation(0, 220, Easing.CUBIC_OUT);
    private final Animation pageAnim = new Animation(1, 260, Easing.CUBIC_OUT);
    private final Animation indicator = new Animation(0, 260, Easing.CUBIC_OUT);
    private final Animation panelSlide = new Animation(0, 280, Easing.CUBIC_OUT);
    private final Animation gridScroll = new Animation(0, 160);
    private float gridScrollTarget, gridContentH;
    private boolean closing, dragging, profileOpen;
    private double dragStartX, dragStartY;
    private float wx, wy, ww, wh;

    private final Map<Module, Animation> hoverAnims = new HashMap<>();
    private final Map<Module, Animation> toggleAnims = new HashMap<>();
    private List<Module> shown = new ArrayList<>();
    private Module selected;
    private SettingsPanel panel;
    private Page page;
    private String tooltip;

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("Void Client"), parent, 620, 360);
        search.onChange(s -> {
            gridScrollTarget = 0;
            rebuild();
        });
    }

    public ClickGuiScreen(Screen parent, Category start) {
        this(parent);
        this.category = start;
    }

    @Override
    protected void layout() {
        ww = Math.min(vw - 24, 800);
        wh = Math.min(vh - 24, 470);
        clampWindow();
        if (open.target() == 0 && !closing) {
            open.target(1);
            fade.target(1);
            VoidClient.get().clientSettings().openSound.play();
        }
        if (lastSelected != null && selected == null) VoidClient.get().modules().byName(lastSelected).ifPresent(this::select);
        indicator.snap(sidebarIndex(category));
        rebuild();
        buildPage();
    }

    private void clampWindow() {
        wx = Math.max(4, Math.min(vw - ww - 4, (vw - ww) / 2f + offsetX));
        wy = Math.max(4, Math.min(vh - wh - 4, (vh - wh) / 2f + offsetY));
    }

    @Override
    public boolean shouldPause() { return VoidClient.get().clientSettings().pauseSingleplayer.get(); }

    // ---- data ---------------------------------------------------------------------------------

    private void rebuild() {
        ModuleManager mm = VoidClient.get().modules();
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Module> base;
        if (!q.isEmpty()) {
            base = new ArrayList<>();
            for (Module m : mm.all()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) base.add(m);
            }
        } else if (category.modules) {
            base = mm.inCategory(category);
        } else {
            base = List.of();
        }
        var state = VoidClient.get().clientState();
        List<Module> out = new ArrayList<>();
        switch (filter) {
            case ALL -> out.addAll(base);
            case ENABLED -> base.stream().filter(Module::isEnabled).forEach(out::add);
            case FAVORITES -> base.stream().filter(m -> state.isFavorite(m.getName())).forEach(out::add);
            case RECENT -> {
                for (String n : state.recent()) base.stream().filter(m -> m.getName().equals(n)).findFirst().ifPresent(out::add);
            }
        }
        shown = out;
    }

    private void buildPage() {
        page = switch (category) {
            case DASHBOARD -> new DashboardPage(this::openModule);
            case HUD_EDITOR -> new HudPage(this);
            case PROFILES -> new ProfilesPage();
            case SETTINGS -> new SettingsPage();
            default -> null;
        };
    }

    private void select(Module m) {
        if (selected == m) return;
        selected = m;
        lastSelected = m == null ? null : m.getName();
        panel = m == null ? null : new SettingsPanel(m.getSettings(), m);
        panelSlide.snap(0);
        panelSlide.target(1);
    }

    /** Jump to a module (used by dashboard shortcuts and search). */
    public void openModule(Module m) {
        switchCategory(m.getCategory());
        select(m);
    }

    private void switchCategory(Category c) {
        if (c == category) return;
        category = c;
        lastCategory = c;
        filter = Filter.ALL;
        gridScrollTarget = 0;
        gridScroll.snap(0);
        indicator.target(sidebarIndex(c));
        pageAnim.snap(0);
        pageAnim.target(1);
        rebuild();
        buildPage();
        VoidClient.get().clientSettings().categorySound.play();
    }

    private static int sidebarIndex(Category c) { return c.ordinal(); }

    // ---- rendering ----------------------------------------------------------------------------

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        tooltip = null;
        float o = open.get();
        float a = fade.get();
        if (closing && fade.isDone()) {
            super.close();
            return;
        }
        Theme t = ThemeManager.get();
        var m = ctx.getMatrices();
        m.pushMatrix();
        float s = 0.9f + 0.1f * o;
        m.translate(wx + ww / 2f, wy + wh / 2f);
        m.scale(s, s);
        m.translate(-(wx + ww / 2f), -(wy + wh / 2f));

        // window
        int accent = ThemeManager.accent(0);
        Render2D.shadow(ctx, wx, wy, ww, wh, 10, 26, ColorUtil.multiplyAlpha(0xA0000000, a));
        Render2D.glow(ctx, wx, wy, ww, wh, 10, 0.9f * a * ThemeManager.glowStrength(), ColorUtil.multiplyAlpha(accent, 0.5f));
        Render2D.roundRect(ctx, wx, wy, ww, wh, 10, ColorUtil.multiplyAlpha(ThemeManager.panel(t.background()), a));
        Render2D.roundRectOutlineGradient(ctx, wx, wy, ww, wh, 10, 1f, ColorUtil.multiplyAlpha(accent, a * 0.9f), ColorUtil.multiplyAlpha(ThemeManager.accent(0.5f), a * 0.9f));

        renderTopBar(ctx, mx, my, delta, a);
        renderSidebar(ctx, mx, my, a);
        renderBottomBar(ctx, a);

        float cx = wx + SIDEBAR, cy = wy + TOP, cw = ww - SIDEBAR, ch = wh - TOP - BOTTOM;
        float pa = pageAnim.get();
        m.pushMatrix();
        m.translate(0, (1 - pa) * 10);
        boolean searching = !search.getText().isBlank();
        if (page != null && !searching) {
            page.bounds(cx + 8, cy + 8, cw - 16, ch - 16);
            page.render(ctx, mx, my, delta);
            tooltip = page.tooltip();
        } else {
            renderModules(ctx, mx, my, delta, cx, cy, cw - RIGHT, ch);
            renderSettings(ctx, mx, my, delta, cx + cw - RIGHT, cy, RIGHT, ch);
        }
        m.popMatrix();

        if (profileOpen) renderProfileDropdown(ctx, mx, my);
        m.popMatrix();

        if (tooltip != null) UI.tooltip(ctx, tooltip, (float) mx, (float) my, vw, vh);
    }

    private void renderTopBar(DrawContext ctx, double mx, double my, float delta, float a) {
        Theme t = ThemeManager.get();
        Render2D.roundRectGradient(ctx, wx, wy, ww, TOP, 10, ColorUtil.withAlpha(t.surface(), 200), ColorUtil.withAlpha(t.surface(), 200), ColorUtil.withAlpha(t.surface(), 120), ColorUtil.withAlpha(t.surface(), 120));
        Render2D.rect(ctx, wx + 10, wy + TOP - 1, ww - 20, 0.6f, ColorUtil.withAlpha(ThemeManager.accent(0), 90));
        // logo
        float lx = wx + 12;
        FontRenderer.drawGradient(ctx, "VOID", lx, wy + 8, ThemeManager.accent(0), ThemeManager.accent(0.33f), FontRenderer.Family.INTER_BOLD, false);
        float lw = FontRenderer.width("VOID", FontRenderer.Family.INTER_BOLD);
        UI.text(ctx, "CLIENT", lx + lw + 4, wy + 8, t.textDim());
        Render2D.glow(ctx, lx, wy + 8, lw, 8, 2, 0.6f * ThemeManager.glowStrength(), ColorUtil.withAlpha(ThemeManager.accent(0), 80));
        // search
        float sx = wx + SIDEBAR + 8, sw = Math.min(220, ww - SIDEBAR - 260);
        search.bounds(sx + 14, wy + 7, sw - 14, 16);
        UI.icon(ctx, "search", sx + 1, wy + 10, 10, t.textDim());
        search.render(ctx, mx, my, delta);
        // active count
        long active = VoidClient.get().modules().enabledCount();
        String act = active + " active";
        float ax = sx + sw + 10;
        float aw = UI.width(act) + 14;
        Render2D.roundRect(ctx, ax, wy + 8, aw, 14, 7, ColorUtil.withAlpha(ThemeManager.accent(0), 50));
        Render2D.circle(ctx, ax + 6, wy + 15, 2, t.success());
        UI.text(ctx, act, ax + 10, wy + 11, t.text());
        // profile selector
        String prof = VoidClient.get().profiles().active();
        float pw = Math.max(80, UI.width(prof) + 26);
        float px = wx + ww - pw - 54;
        boolean ph = mx >= px && mx < px + pw && my >= wy + 7 && my < wy + 23;
        Render2D.roundRect(ctx, px, wy + 7, pw, 16, 5, ph || profileOpen ? t.surfaceHover() : ColorUtil.withAlpha(t.surfaceHover(), 150));
        Render2D.roundRectOutline(ctx, px, wy + 7, pw, 16, 5, 0.7f, profileOpen ? ThemeManager.accent(0) : t.stroke());
        UI.icon(ctx, "profiles", px + 4, wy + 10, 9, ThemeManager.accent(0));
        UI.text(ctx, UI.trim(prof, pw - 22), px + 16, wy + 11, t.text());
        // settings + close
        float gx = wx + ww - 46;
        boolean gh = mx >= gx && mx < gx + 16 && my >= wy + 7 && my < wy + 23;
        if (gh) Render2D.circle(ctx, gx + 8, wy + 15, 9, t.surfaceHover());
        UI.icon(ctx, "settings", gx + 2, wy + 9, 12, gh ? ThemeManager.accent(0) : t.textDim());
        float xx = wx + ww - 24;
        boolean xh = mx >= xx && mx < xx + 16 && my >= wy + 7 && my < wy + 23;
        if (xh) Render2D.circle(ctx, xx + 8, wy + 15, 9, ColorUtil.withAlpha(t.danger(), 70));
        UI.icon(ctx, "close", xx + 3, wy + 10, 10, xh ? t.danger() : t.textDim());
        if (gh) tooltip = "Client settings";
        if (xh) tooltip = "Close (Esc)";
    }

    private void renderSidebar(DrawContext ctx, double mx, double my, float a) {
        Theme t = ThemeManager.get();
        float sx = wx, sy = wy + TOP, sh = wh - TOP - BOTTOM;
        Render2D.rect(ctx, sx + SIDEBAR - 0.6f, sy + 6, 0.6f, sh - 12, ColorUtil.withAlpha(t.stroke(), 120));
        Category[] cats = Category.values();
        float item = Math.min(24, (sh - 10) / cats.length);
        float iy = sy + 5;
        float ind = indicator.get();
        float indY = iy + ind * item;
        Render2D.roundRectH(ctx, sx + 6, indY + 1, SIDEBAR - 12, item - 2, 6, ColorUtil.withAlpha(ThemeManager.accent(0), 70), ColorUtil.withAlpha(ThemeManager.accent(0.33f), 20));
        Render2D.roundRect(ctx, sx + 6, indY + 5, 2.5f, item - 10, 1.2f, ThemeManager.accent(0));
        for (Category c : cats) {
            boolean active = c == category;
            boolean hover = mx >= sx + 6 && mx < sx + SIDEBAR - 6 && my >= iy && my < iy + item;
            if (hover && !active) Render2D.roundRect(ctx, sx + 6, iy + 1, SIDEBAR - 12, item - 2, 6, ColorUtil.withAlpha(t.surfaceHover(), 160));
            int col = active ? 0xFFFFFFFF : hover ? t.text() : t.textDim();
            UI.icon(ctx, c.icon, sx + 15, iy + (item - 11) / 2f, 11, active ? ThemeManager.accent(0) : col);
            UI.text(ctx, c.displayName, sx + 32, iy + (item - 8) / 2f, col);
            if (c.modules) {
                long on = VoidClient.get().modules().inCategory(c).stream().filter(Module::isEnabled).count();
                if (on > 0) {
                    String n = String.valueOf(on);
                    UI.small(ctx, n, sx + SIDEBAR - 14 - UI.smallWidth(n), iy + (item - 6) / 2f, active ? ThemeManager.accent(0) : t.textDim());
                }
            }
            iy += item;
        }
    }

    private void renderBottomBar(DrawContext ctx, float a) {
        Theme t = ThemeManager.get();
        float by = wy + wh - BOTTOM;
        Render2D.rect(ctx, wx + 10, by, ww - 20, 0.6f, ColorUtil.withAlpha(t.stroke(), 140));
        int ping = -1;
        if (client.player != null && client.getNetworkHandler() != null) {
            var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String left = "Minecraft " + VoidClient.MC_VERSION + "   •   Void " + VoidClient.VERSION;
        String right = client.getCurrentFps() + " FPS   •   " + (ping < 0 ? "-" : ping + " ms") + "   •   " + VoidClient.get().profiles().active();
        UI.small(ctx, left, wx + 12, by + 5, t.textDim());
        UI.small(ctx, right, wx + ww - 12 - UI.smallWidth(right), by + 5, t.textDim());
    }

    private void renderModules(DrawContext ctx, double mx, double my, float delta, float x, float y, float w, float h) {
        Theme t = ThemeManager.get();
        String title = search.getText().isBlank() ? category.displayName : "Search: " + search.getText();
        UI.bold(ctx, title, x + 10, y + 8, t.text());
        String count = shown.size() + " modules";
        UI.small(ctx, count, x + 12 + UI.boldWidth(title) + 6, y + 9.5f, t.textDim());
        // filter chips
        float fx = x + w - 10;
        Filter[] filters = Filter.values();
        for (int i = filters.length - 1; i >= 0; i--) {
            String label = filters[i].name().charAt(0) + filters[i].name().substring(1).toLowerCase(Locale.ROOT);
            float cw = UI.chipWidth(label);
            fx -= cw;
            UI.chip(ctx, fx, y + 6, cw, 12, label, filter == filters[i], mx >= fx && mx < fx + cw && my >= y + 6 && my < y + 18);
            fx -= 3;
        }
        float gy = y + 24, gh = h - 28;
        int cols = w > 360 ? 2 : 1;
        float gap = 6;
        float cardW = (w - 20 - gap * (cols - 1)) / cols;
        boolean desc = VoidClient.get().clientSettings().showDescriptions.get();
        float cardH = desc ? 44 : 28;
        int rows = (shown.size() + cols - 1) / cols;
        gridContentH = rows * (cardH + gap);
        gridScrollTarget = Math.max(0, Math.min(Math.max(0, gridContentH - gh), gridScrollTarget));
        gridScroll.target(gridScrollTarget);
        float off = gridScroll.get();
        Render2D.scissor(ctx, x, gy, w, gh);
        boolean inside = mx >= x && mx < x + w && my >= gy && my < gy + gh;
        for (int i = 0; i < shown.size(); i++) {
            Module mod = shown.get(i);
            float cx = x + 10 + (i % cols) * (cardW + gap);
            float cy = gy + (i / cols) * (cardH + gap) - off;
            if (cy + cardH < gy - 2 || cy > gy + gh + 2) continue;
            boolean hover = inside && mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH;
            renderCard(ctx, mod, cx, cy, cardW, cardH, hover, mx, my, desc, i);
        }
        Render2D.endScissor(ctx);
        if (shown.isEmpty()) {
            String msg = filter == Filter.FAVORITES ? "No favourites yet - click a card's star." : filter == Filter.RECENT ? "Nothing used recently." : "No modules match.";
            UI.centered(ctx, msg, x + w / 2f, gy + 40, t.textDim());
        }
        if (gridContentH > gh) {
            float barH = Math.max(16, gh * gh / gridContentH);
            float barY = gy + (gh - barH) * (off / Math.max(1, gridContentH - gh));
            Render2D.roundRect(ctx, x + w - 4, barY, 2, barH, 1, ColorUtil.withAlpha(ThemeManager.accent(0), 140));
        }
    }

    private void renderCard(DrawContext ctx, Module mod, float x, float y, float w, float h, boolean hover, double mx, double my, boolean desc, int index) {
        Theme t = ThemeManager.get();
        Animation ha = hoverAnims.computeIfAbsent(mod, k -> new Animation(0, 160));
        Animation ta = toggleAnims.computeIfAbsent(mod, k -> new Animation(mod.isEnabled() ? 1 : 0, 220, Easing.BACK_OUT));
        ha.target(hover ? 1 : 0);
        ta.target(mod.isEnabled() ? 1 : 0);
        float hv = ha.get(), tv = (float) Math.max(0, Math.min(1, ta.update()));
        boolean sel = mod == selected;
        int accent = ThemeManager.accent(index * 0.04f);
        if (tv > 0.02f || sel) Render2D.glow(ctx, x, y, w, h, 6, (0.35f * tv + (sel ? 0.4f : 0)) * ThemeManager.glowStrength(), accent);
        int bg = ColorUtil.lerp(ThemeManager.panel(t.surface()), t.surfaceHover(), hv * 0.8f);
        Render2D.roundRect(ctx, x, y - hv, w, h, 6, bg);
        if (tv > 0.01f) Render2D.roundRectH(ctx, x, y - hv, w, h, 6, ColorUtil.withAlpha(accent, (int) (40 * tv)), ColorUtil.withAlpha(accent, 0));
        Render2D.roundRectOutline(ctx, x, y - hv, w, h, 6, sel ? 1.1f : 0.7f, sel ? accent : ColorUtil.lerp(t.stroke(), ColorUtil.withAlpha(accent, 150), Math.max(hv, tv * 0.6f)));
        float ty = y - hv;
        UI.bold(ctx, UI.trim(mod.getName(), w - 64), x + 9, ty + 7, mod.isEnabled() ? 0xFFFFFFFF : t.text());
        if (desc) {
            List<String> lines = UI.wrap(mod.getDescription(), (w - 18) / 1f);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                String line = lines.get(i);
                if (i == 1 && lines.size() > 2) line = UI.trim(line + "…", (w - 18) / 0.75f);
                UI.small(ctx, line, x + 9, ty + 19 + i * 8, t.textDim());
            }
        }
        if (mod.isToggleable()) {
            UI.toggle(ctx, x + w - 30, ty + 6, 22, 11, tv, hover && mx >= x + w - 34);
        } else {
            UI.chip(ctx, x + w - 38, ty + 5, 32, 12, "Open", false, hover && mx >= x + w - 40);
        }
        boolean fav = VoidClient.get().clientState().isFavorite(mod.getName());
        float sx = x + w - 46;
        boolean sh = hover && mx >= sx && mx < sx + 12 && my >= ty + 4 && my < ty + 18;
        if (fav || hover) UI.icon(ctx, "star", sx, ty + 6, 10, fav ? t.warning() : sh ? t.text() : ColorUtil.withAlpha(t.textDim(), 140));
        if (mod.keybind.get().isBound()) {
            String kb = mod.keybind.get().displayName();
            UI.small(ctx, kb, x + w - 8 - UI.smallWidth(kb), ty + h - 9, ColorUtil.withAlpha(accent, 200));
        }
        if (hover && desc && mx < x + w - 50) tooltip = mod.getDescription();
    }

    private void renderSettings(DrawContext ctx, double mx, double my, float delta, float x, float y, float w, float h) {
        Theme t = ThemeManager.get();
        Render2D.rect(ctx, x, y + 6, 0.6f, h - 12, ColorUtil.withAlpha(t.stroke(), 120));
        if (selected == null) {
            UI.centered(ctx, "Select a module", x + w / 2f, y + h / 2f - 8, t.textDim());
            UI.centered(ctx, "to edit its settings", x + w / 2f, y + h / 2f + 4, ColorUtil.withAlpha(t.textDim(), 150));
            return;
        }
        float slide = panelSlide.get();
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((1 - slide) * 30, 0);
        float px = x + 10, pw = w - 18;
        UI.bold(ctx, UI.trim(selected.getName(), pw - 60), px, y + 8, 0xFFFFFFFF);
        if (selected.isToggleable()) {
            Animation ta = toggleAnims.computeIfAbsent(selected, k -> new Animation(selected.isEnabled() ? 1 : 0, 220, Easing.BACK_OUT));
            UI.toggle(ctx, px + pw - 24, y + 6, 22, 11, (float) Math.max(0, Math.min(1, ta.update())), false);
        } else {
            UI.button(ctx, px + pw - 40, y + 5, 40, 13, "Open", mx >= px + pw - 40 && mx < px + pw && my >= y + 5 && my < y + 18, true);
        }
        List<String> lines = UI.wrap(selected.getDescription(), pw);
        float dy = y + 21;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            UI.small(ctx, lines.get(i), px, dy, t.textDim());
            dy += 8;
        }
        float previewH = 0;
        if (selected.hasPreview()) {
            previewH = 58;
            Render2D.roundRect(ctx, px, dy + 3, pw, previewH - 6, 5, ColorUtil.withAlpha(0xFF000000, 120));
            Render2D.roundRectOutline(ctx, px, dy + 3, pw, previewH - 6, 5, 0.6f, t.stroke());
            UI.small(ctx, "LIVE PREVIEW", px + 4, dy + 5, ColorUtil.withAlpha(ThemeManager.accent(0), 200));
            Render2D.scissor(ctx, px, dy + 3, pw, previewH - 6);
            selected.renderPreview(ctx, px, dy + 3, pw, previewH - 6, delta);
            Render2D.endScissor(ctx);
        }
        float listY = dy + 4 + previewH;
        float resetY = y + h - 20;
        panel.bounds(px - 2, listY, pw + 6, resetY - listY - 4);
        panel.render(ctx, mx, my, delta);
        if (panel.hoveredComponent() != null) tooltip = panel.hoveredComponent().tooltip();
        UI.button(ctx, px, resetY, pw / 2f - 2, 14, "Reset", mx >= px && mx < px + pw / 2f - 2 && my >= resetY && my < resetY + 14, false);
        boolean fav = VoidClient.get().clientState().isFavorite(selected.getName());
        UI.button(ctx, px + pw / 2f + 2, resetY, pw / 2f - 2, 14, fav ? "Unfavourite" : "Favourite", mx >= px + pw / 2f + 2 && mx < px + pw && my >= resetY && my < resetY + 14, false);
        m.popMatrix();
    }

    private void renderProfileDropdown(DrawContext ctx, double mx, double my) {
        Theme t = ThemeManager.get();
        List<String> profiles = VoidClient.get().profiles().list();
        String prof = VoidClient.get().profiles().active();
        float pw = Math.max(110, UI.width(prof) + 26);
        float px = wx + ww - Math.max(80, UI.width(prof) + 26) - 54;
        float py = wy + 26;
        float h = profiles.size() * 14 + 4;
        Render2D.shadow(ctx, px, py, pw, h, 5, 10, 0x90000000);
        Render2D.roundRect(ctx, px, py, pw, h, 5, ColorUtil.withAlpha(t.background(), 250));
        Render2D.roundRectOutline(ctx, px, py, pw, h, 5, 0.8f, ThemeManager.accent(0));
        float ry = py + 2;
        for (String p : profiles) {
            boolean hov = mx >= px && mx < px + pw && my >= ry && my < ry + 14;
            if (hov || p.equals(prof)) Render2D.roundRect(ctx, px + 2, ry, pw - 4, 13, 3, p.equals(prof) ? ColorUtil.withAlpha(ThemeManager.accent(0), 70) : t.surfaceHover());
            UI.text(ctx, UI.trim(p, pw - 12), px + 6, ry + 3, p.equals(prof) ? 0xFFFFFFFF : t.text());
            ry += 14;
        }
    }

    // ---- input ------------------------------------------------------------------------------

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (closing) return true;
        ModuleManager mm = VoidClient.get().modules();
        if (profileOpen) {
            List<String> profiles = VoidClient.get().profiles().list();
            String prof = VoidClient.get().profiles().active();
            float pw = Math.max(110, UI.width(prof) + 26);
            float px = wx + ww - Math.max(80, UI.width(prof) + 26) - 54, py = wy + 26;
            profileOpen = false;
            if (mx >= px && mx < px + pw && my >= py) {
                int idx = (int) ((my - py - 2) / 14);
                if (idx >= 0 && idx < profiles.size()) {
                    VoidClient.get().profiles().switchTo(profiles.get(idx));
                    select(null);
                    rebuild();
                    buildPage();
                }
            }
            ModuleManager.playClick();
            return true;
        }
        // top bar
        if (my >= wy && my < wy + TOP && mx >= wx && mx < wx + ww) {
            String prof = VoidClient.get().profiles().active();
            float pw = Math.max(80, UI.width(prof) + 26), px = wx + ww - pw - 54;
            if (search.mouseClicked(mx, my, button)) return true;
            if (mx >= px && mx < px + pw) {
                profileOpen = true;
                ModuleManager.playClick();
                return true;
            }
            if (mx >= wx + ww - 46 && mx < wx + ww - 30) {
                switchCategory(Category.SETTINGS);
                return true;
            }
            if (mx >= wx + ww - 24 && mx < wx + ww - 8) {
                close();
                return true;
            }
            dragging = true;
            dragStartX = mx - offsetX;
            dragStartY = my - offsetY;
            return true;
        }
        search.setFocused(false);
        // sidebar
        float sy = wy + TOP, sh = wh - TOP - BOTTOM;
        if (mx >= wx && mx < wx + SIDEBAR && my >= sy && my < sy + sh) {
            Category[] cats = Category.values();
            float item = Math.min(24, (sh - 10) / cats.length);
            int idx = (int) ((my - sy - 5) / item);
            if (idx >= 0 && idx < cats.length) {
                search.setText("");
                switchCategory(cats[idx]);
            }
            return true;
        }
        float cx = wx + SIDEBAR, cy = wy + TOP, cw = ww - SIDEBAR, ch = wh - TOP - BOTTOM;
        boolean searching = !search.getText().isBlank();
        if (page != null && !searching) return page.mouseClicked(mx, my, button);
        // settings panel
        float rx = cx + cw - RIGHT;
        if (mx >= rx && selected != null) {
            float px = rx + 10, pw = RIGHT - 18;
            if (my >= cy + 4 && my < cy + 19 && mx >= px + pw - 40) {
                mm.userToggle(selected, false);
                return true;
            }
            float resetY = cy + ch - 20;
            if (my >= resetY && my < resetY + 14) {
                if (mx < px + pw / 2f) {
                    selected.resetSettings();
                    panel = new SettingsPanel(selected.getSettings(), selected);
                } else {
                    VoidClient.get().clientState().toggleFavorite(selected.getName());
                    rebuild();
                }
                ModuleManager.playClick();
                return true;
            }
            return panel.mouseClicked(mx, my, button);
        }
        // filter chips
        float fx = rx - 10;
        Filter[] filters = Filter.values();
        if (my >= cy + 6 && my < cy + 18) {
            for (int i = filters.length - 1; i >= 0; i--) {
                String label = filters[i].name().charAt(0) + filters[i].name().substring(1).toLowerCase(Locale.ROOT);
                float w = UI.chipWidth(label);
                fx -= w;
                if (mx >= fx && mx < fx + w) {
                    filter = filters[i];
                    gridScrollTarget = 0;
                    rebuild();
                    ModuleManager.playClick();
                    return true;
                }
                fx -= 3;
            }
        }
        // cards
        float gy = cy + 24, gh = ch - 28, w = cw - RIGHT;
        if (my < gy || my >= gy + gh) return false;
        int cols = w > 360 ? 2 : 1;
        float gap = 6, cardW = (w - 20 - gap * (cols - 1)) / cols;
        float cardH = VoidClient.get().clientSettings().showDescriptions.get() ? 44 : 28;
        float off = gridScroll.get();
        for (int i = 0; i < shown.size(); i++) {
            float x = cx + 10 + (i % cols) * (cardW + gap);
            float y = gy + (i / cols) * (cardH + gap) - off;
            if (mx >= x && mx < x + cardW && my >= y && my < y + cardH) {
                Module mod = shown.get(i);
                if (button == 2) {
                    VoidClient.get().clientState().toggleFavorite(mod.getName());
                } else if (button == 1) {
                    mm.userToggle(mod, false);
                } else if (mx >= x + cardW - 46 && mx < x + cardW - 34 && my < y + 18) {
                    VoidClient.get().clientState().toggleFavorite(mod.getName());
                    ModuleManager.playClick();
                    if (filter == Filter.FAVORITES) rebuild();
                } else if (mx >= x + cardW - 36 && my < y + 20) {
                    mm.userToggle(mod, false);
                    select(mod);
                } else {
                    select(mod);
                    ModuleManager.playClick();
                }
                if (filter == Filter.ENABLED) rebuild();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        dragging = false;
        if (page != null && search.getText().isBlank()) return page.mouseReleased(mx, my, button);
        return panel != null && panel.mouseReleased(mx, my, button);
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            offsetX = (float) (mx - dragStartX);
            offsetY = (float) (my - dragStartY);
            clampWindow();
            offsetX = wx - (vw - ww) / 2f;
            offsetY = wy - (vh - wh) / 2f;
            return true;
        }
        if (page != null && search.getText().isBlank()) return page.mouseDragged(mx, my, button, dx, dy);
        return false;
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        if (page != null && search.getText().isBlank()) return page.mouseScrolled(mx, my, amount);
        float cx = wx + SIDEBAR, cw = ww - SIDEBAR;
        if (mx >= cx + cw - RIGHT) return panel != null && panel.mouseScrolled(mx, my, amount);
        gridScrollTarget -= (float) amount * 30;
        return true;
    }

    @Override
    protected boolean onKey(int key, int scancode, int mods) {
        if (search.capturesKeyboard()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                search.setText("");
                search.setFocused(false);
                rebuild();
                return true;
            }
            return search.keyPressed(key, scancode, mods);
        }
        if (page != null && search.getText().isBlank() && page.capturesKeyboard()) return page.keyPressed(key, scancode, mods);
        if (panel != null && panel.capturesKeyboard()) return panel.keyPressed(key, scancode, mods);
        if (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            search.setFocused(true);
            return true;
        }
        if (VoidClient.get().openGuiKey().matchesKey(new net.minecraft.client.input.KeyInput(key, scancode, mods))) {
            close();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onChar(String chars) {
        if (search.capturesKeyboard()) return search.charTyped(chars);
        if (page != null && search.getText().isBlank() && page.capturesKeyboard()) return page.charTyped(chars);
        if (panel != null && panel.capturesKeyboard()) return panel.charTyped(chars);
        if (!chars.isBlank()) {
            search.setFocused(true);
            return search.charTyped(chars);
        }
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        open.target(0);
        fade.target(0);
        VoidClient.get().clientSettings().closeSound.play();
        VoidClient.get().config().saveAll();
    }

    @Override
    public void removed() {
        lastCategory = category;
    }
}
