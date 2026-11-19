package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.ModuleManager;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.List;

/** The Void title screen: animated background, logo, staggered buttons and profile picker. */
public class MainMenuScreen extends VoidScreen {
    private static final Identifier LOGO = Identifier.of("voidclient", "textures/gui/logo.png");
    private static final String[] TIPS = {
            "Right Shift opens the Void GUI in game.",
            "Right-click any setting to reset it.",
            "Every module has its own keybind - hold mode included.",
            "Profiles store modules, HUD layout, theme and keybinds.",
            "The HUD editor snaps to a grid and shows alignment guides.",
            "Crystal and Anchor trainers run in singleplayer worlds."
    };

    private record Entry(String label, String icon, Runnable action) {}

    private List<Entry> entries;
    private Animation[] intro;
    private Animation[] hover;
    private final Animation fadeIn = new Animation(0, 900, Easing.CUBIC_OUT);
    private boolean profileOpen;
    private float bx, by, bw, bh, gap;

    public MainMenuScreen() {
        super(Text.literal("Void Client"), null, 420, 260);
    }

    @Override
    protected void layout() {
        entries = List.of(
                new Entry("Singleplayer", "player", () -> client.setScreen(new SelectWorldScreen(this))),
                new Entry("Multiplayer", "world", () -> client.setScreen(new MultiplayerScreen(this))),
                new Entry("Realms", "dashboard", () -> client.setScreen(new RealmsMainScreen(this))),
                new Entry("Options", "settings", () -> client.setScreen(new OptionsScreen(this, client.options))),
                new Entry("Client Settings", "visuals", () -> client.setScreen(new ClickGuiScreen(this, Category.SETTINGS))),
                new Entry("Quit Game", "close", () -> client.scheduleStop())
        );
        if (intro == null) {
            intro = new Animation[entries.size()];
            hover = new Animation[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                intro[i] = new Animation(0, 520 + i * 70, Easing.BACK_OUT);
                intro[i].target(1);
                hover[i] = new Animation(0, 160);
            }
            fadeIn.target(1);
        }
        bw = 170;
        bh = 22;
        gap = 6;
        bx = Math.max(24, vw * 0.09f);
        by = vh * 0.40f;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        MenuBackground.setMouse((float) mx, (float) my);
        Theme t = ThemeManager.get();
        double time = (System.currentTimeMillis() % 1_000_000L) / 1000.0;

        // logo
        float lw = Math.min(300, vw * 0.42f), lh = lw / 4f;
        float lx = bx - 12, ly = vh * 0.14f + (float) Math.sin(time * 1.2) * 2.5f;
        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 2.0));
        Render2D.radialGlow(ctx, lx + lw * 0.5f, ly + lh * 0.5f, lw * 0.62f, lh * 1.1f, ColorUtil.withAlpha(ThemeManager.accent(0), (int) (34 + pulse * 22)));
        Render2D.texture(ctx, LOGO, lx, ly, lw, lh, 0xFFFFFFFF);
        FontRenderer.drawGradient(ctx, "C L I E N T", lx + lw * 0.32f, ly + lh * 0.86f, ThemeManager.accent(0), ThemeManager.accent(0.5f), FontRenderer.Family.INTER_BOLD, false);
        UI.small(ctx, "Minecraft " + VoidClient.MC_VERSION + " Edition", lx + lw * 0.32f, ly + lh * 0.86f + 11, t.textDim());

        // buttons
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            float in = intro[i].get();
            float y = by + i * (bh + gap);
            float x = bx - (1 - Math.min(1, in)) * 60;
            boolean h = mx >= bx && mx < bx + bw && my >= y && my < y + bh;
            hover[i].target(h ? 1 : 0);
            float hv = hover[i].get();
            float alpha = Math.min(1f, Math.max(0f, in));
            int accent = ThemeManager.accent(i * 0.08f);
            if (hv > 0.01f) Render2D.glow(ctx, x + hv * 6, y, bw, bh, 6, hv * ThemeManager.glowStrength(), accent);
            Render2D.roundRect(ctx, x + hv * 6, y, bw, bh, 6, ColorUtil.multiplyAlpha(ColorUtil.lerp(ColorUtil.withAlpha(t.surface(), 170), t.surfaceHover(), hv), alpha));
            Render2D.roundRectH(ctx, x + hv * 6, y, bw * (0.25f + 0.75f * hv), bh, 6, ColorUtil.multiplyAlpha(ColorUtil.withAlpha(accent, 70), alpha * hv), ColorUtil.withAlpha(accent, 0));
            Render2D.roundRectOutline(ctx, x + hv * 6, y, bw, bh, 6, 0.8f, ColorUtil.multiplyAlpha(ColorUtil.lerp(t.stroke(), accent, hv), alpha));
            Render2D.roundRect(ctx, x + hv * 6 + 3, y + 5, 2, bh - 10, 1, ColorUtil.multiplyAlpha(accent, alpha * (0.4f + 0.6f * hv)));
            UI.icon(ctx, e.icon, x + hv * 6 + 11, y + 6, 10, ColorUtil.multiplyAlpha(hv > 0.5f ? accent : t.textDim(), alpha));
            UI.bold(ctx, e.label, x + hv * 6 + 28, y + 7, ColorUtil.multiplyAlpha(t.text(), alpha));
            if (hv > 0.05f) {
                float cx = x + bw - 10 + hv * 6;
                Render2D.triangle(ctx, cx - 2, y + 7.5f, cx + 2.5f, y + 11, cx - 2, y + 14.5f, ColorUtil.multiplyAlpha(accent, hv * alpha));
            }
        }

        // profile picker
        String prof = VoidClient.get().profiles().active();
        float px = bx, py = by + entries.size() * (bh + gap) + 10, pw = bw;
        boolean ph = mx >= px && mx < px + pw && my >= py && my < py + 16;
        Render2D.roundRect(ctx, px, py, pw, 16, 5, ph || profileOpen ? t.surfaceHover() : ColorUtil.withAlpha(t.surface(), 150));
        Render2D.roundRectOutline(ctx, px, py, pw, 16, 5, 0.7f, profileOpen ? ThemeManager.accent(0) : t.stroke());
        UI.small(ctx, "PROFILE", px + 7, py + 5, t.textDim());
        UI.text(ctx, UI.trim(prof, pw - 60), px + 44, py + 4, t.text());
        if (profileOpen) {
            List<String> list = VoidClient.get().profiles().list();
            float ly2 = py + 18;
            Render2D.roundRect(ctx, px, ly2, pw, list.size() * 13 + 4, 5, ColorUtil.withAlpha(t.background(), 245));
            Render2D.roundRectOutline(ctx, px, ly2, pw, list.size() * 13 + 4, 5, 0.7f, ThemeManager.accent(0));
            float ry = ly2 + 2;
            for (String p : list) {
                boolean hov = mx >= px && mx < px + pw && my >= ry && my < ry + 13;
                if (hov || p.equals(prof)) Render2D.roundRect(ctx, px + 2, ry, pw - 4, 12, 3, p.equals(prof) ? ColorUtil.withAlpha(ThemeManager.accent(0), 70) : t.surfaceHover());
                UI.text(ctx, p, px + 7, ry + 2.5f, t.text());
                ry += 13;
            }
        }

        // footer
        String tip = TIPS[(int) ((System.currentTimeMillis() / 6000) % TIPS.length)];
        UI.small(ctx, tip, bx, vh - 14, ColorUtil.withAlpha(t.textDim(), 200));
        String ver = "Void Client " + VoidClient.VERSION + "  •  Fabric  •  " + VoidClient.MC_VERSION;
        UI.small(ctx, ver, vw - 10 - UI.smallWidth(ver), vh - 14, t.textDim());
        String vanilla = "Vanilla Menu";
        float vwid = UI.width(vanilla) + 14;
        UI.button(ctx, vw - vwid - 10, 10, vwid, 15, vanilla, mx >= vw - vwid - 10 && mx < vw - 10 && my >= 10 && my < 25, false);

        float f = fadeIn.get();
        if (f < 1f) Render2D.rect(ctx, 0, 0, vw, vh, ColorUtil.withAlpha(0xFF000000, (int) ((1 - f) * 255)));
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        float px = bx, py = by + entries.size() * (bh + gap) + 10, pw = bw;
        if (profileOpen) {
            profileOpen = false;
            List<String> list = VoidClient.get().profiles().list();
            float ly2 = py + 18;
            if (mx >= px && mx < px + pw && my >= ly2) {
                int idx = (int) ((my - ly2 - 2) / 13);
                if (idx >= 0 && idx < list.size()) VoidClient.get().profiles().switchTo(list.get(idx));
            }
            ModuleManager.playClick();
            return true;
        }
        if (mx >= px && mx < px + pw && my >= py && my < py + 16) {
            profileOpen = true;
            ModuleManager.playClick();
            return true;
        }
        String vanilla = "Vanilla Menu";
        float vwid = UI.width(vanilla) + 14;
        if (mx >= vw - vwid - 10 && mx < vw - 10 && my >= 10 && my < 25) {
            ModuleManager.playClick();
            ScreenReplacer.bypassNext();
            client.setScreen(new TitleScreen());
            return true;
        }
        for (int i = 0; i < entries.size(); i++) {
            float y = by + i * (bh + gap);
            if (mx >= bx && mx < bx + bw && my >= y && my < y + bh) {
                ModuleManager.playClick();
                entries.get(i).action().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
    }
}
