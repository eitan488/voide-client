package voidclient.gui.screens.pages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.Module;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Overview: live stats, FPS graph, favourites, recently used and notification history. */
public class DashboardPage extends Page {
    private static final int SAMPLES = 120;
    private static final float[] FPS = new float[SAMPLES];
    private static int head;
    private static long lastSample;

    private final Consumer<Module> open;
    private final List<float[]> favHit = new ArrayList<>();
    private final List<Module> favMods = new ArrayList<>();
    private final List<float[]> recentHit = new ArrayList<>();
    private final List<Module> recentMods = new ArrayList<>();
    private String tip;

    public DashboardPage(Consumer<Module> open) { this.open = open; }

    @Override
    public String tooltip() { return tip; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        tip = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        Theme t = ThemeManager.get();
        if (System.currentTimeMillis() - lastSample > 250) {
            lastSample = System.currentTimeMillis();
            FPS[head] = mc.getCurrentFps();
            head = (head + 1) % SAMPLES;
        }
        FontRenderer.drawGradient(ctx, "Welcome to the Void", x + 2, y + 2, ThemeManager.accent(0), ThemeManager.accent(0.33f), FontRenderer.Family.INTER_BOLD, false);
        String who = mc.getSession() != null ? mc.getSession().getUsername() : "player";
        UI.small(ctx, "Signed in as " + who + "  •  profile " + VoidClient.get().profiles().active(), x + 2, y + 14, t.textDim());

        // stat tiles
        var mm = VoidClient.get().modules();
        int ping = -1;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String[][] tiles = {
                {"ACTIVE MODULES", mm.enabledCount() + " / " + mm.all().size()},
                {"SESSION", MathUtil.formatDuration(VoidClient.get().session().sessionMs())},
                {"FPS", String.valueOf(mc.getCurrentFps())},
                {"PING", ping < 0 ? "offline" : ping + " ms"}
        };
        float tw = (w - 3 * 6) / 4f, ty = y + 28;
        for (int i = 0; i < tiles.length; i++) {
            float tx = x + i * (tw + 6);
            Render2D.roundRect(ctx, tx, ty, tw, 36, 6, ThemeManager.panel(t.surface()));
            Render2D.roundRectOutline(ctx, tx, ty, tw, 36, 6, 0.7f, ColorUtil.withAlpha(ThemeManager.accent(i * 0.12f), 120));
            Render2D.roundRect(ctx, tx + 6, ty + 6, 2, 24, 1, ThemeManager.accent(i * 0.12f));
            UI.small(ctx, tiles[i][0], tx + 13, ty + 7, t.textDim());
            UI.bold(ctx, tiles[i][1], tx + 13, ty + 19, 0xFFFFFFFF);
        }

        // fps graph
        float gx = x, gy = ty + 44, gw = w * 0.58f, gh = 70;
        Render2D.roundRect(ctx, gx, gy, gw, gh, 6, ThemeManager.panel(t.surface()));
        UI.small(ctx, "FRAME RATE", gx + 8, gy + 6, t.textDim());
        float max = 30;
        for (float f : FPS) max = Math.max(max, f);
        float px = -1, py = -1;
        for (int i = 0; i < SAMPLES; i++) {
            float v = FPS[(head + i) % SAMPLES];
            float sx = gx + 8 + (gw - 16) * i / (SAMPLES - 1f);
            float sy = gy + gh - 8 - (gh - 22) * (v / max);
            if (px >= 0) {
                Render2D.line(ctx, px, py, sx, sy, 1.2f, ThemeManager.accent(i / (float) SAMPLES * 0.3f));
                Render2D.gradientV(ctx, sx - (sx - px), Math.min(py, sy), sx - px + 0.3f, gy + gh - 8 - Math.min(py, sy), ColorUtil.withAlpha(ThemeManager.accent(0), 40), 0x00000000);
            }
            px = sx;
            py = sy;
        }
        String mxs = Math.round(max) + " max";
        UI.small(ctx, mxs, gx + gw - 8 - UI.smallWidth(mxs), gy + 6, t.textDim());

        // notification history
        float nx = gx + gw + 6, nw = w - gw - 6;
        Render2D.roundRect(ctx, nx, gy, nw, gh, 6, ThemeManager.panel(t.surface()));
        UI.small(ctx, "NOTIFICATIONS", nx + 8, gy + 6, t.textDim());
        List<String> hist = Notifications.history();
        for (int i = 0; i < Math.min(6, hist.size()); i++) {
            UI.small(ctx, UI.trim(hist.get(i), (nw - 16) / 0.75f), nx + 8, gy + 17 + i * 8.5f, t.text());
        }
        if (hist.isEmpty()) UI.small(ctx, "Nothing yet.", nx + 8, gy + 17, t.textDim());

        // favourites and recents
        float fy = gy + gh + 8;
        chipsRow(ctx, mx, my, "FAVOURITES  (click to toggle)", favoriteModules(), fy, favHit, favMods, true);
        chipsRow(ctx, mx, my, "RECENTLY USED  (click to open)", recentModules(), fy + 46, recentHit, recentMods, false);

        float qy = fy + 94;
        if (qy + 16 < y + h) {
            UI.button(ctx, x, qy, 110, 16, "Open HUD Editor", inBox(mx, my, x, qy, 110, 16), true);
            UI.button(ctx, x + 116, qy, 90, 16, "Save Profile", inBox(mx, my, x + 116, qy, 90, 16), false);
            UI.button(ctx, x + 212, qy, 110, 16, "Reset Session Stats", inBox(mx, my, x + 212, qy, 110, 16), false);
        }
    }

    private static boolean inBox(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Module> favoriteModules() {
        List<Module> out = new ArrayList<>();
        for (String n : VoidClient.get().clientState().favorites()) VoidClient.get().modules().byName(n).ifPresent(out::add);
        return out;
    }

    private List<Module> recentModules() {
        List<Module> out = new ArrayList<>();
        for (String n : VoidClient.get().clientState().recent()) VoidClient.get().modules().byName(n).ifPresent(out::add);
        return out;
    }

    private void chipsRow(DrawContext ctx, double mx, double my, String title, List<Module> mods, float yy, List<float[]> hits, List<Module> hitMods, boolean toggleStyle) {
        Theme t = ThemeManager.get();
        hits.clear();
        hitMods.clear();
        Render2D.roundRect(ctx, x, yy, w, 40, 6, ThemeManager.panel(t.surface()));
        UI.small(ctx, title, x + 8, yy + 5, t.textDim());
        float cx = x + 8, cy = yy + 16;
        if (mods.isEmpty()) UI.small(ctx, "Empty.", cx, cy + 3, t.textDim());
        for (Module m : mods) {
            float cw = UI.chipWidth(m.getName());
            if (cx + cw > x + w - 8) break;
            boolean hover = inBox(mx, my, cx, cy, cw, 14);
            UI.chip(ctx, cx, cy, cw, 14, m.getName(), toggleStyle && m.isEnabled(), hover);
            if (hover) tip = m.getDescription();
            hits.add(new float[]{cx, cy, cw, 14});
            hitMods.add(m);
            cx += cw + 4;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < favHit.size(); i++) {
            float[] r = favHit.get(i);
            if (inBox(mx, my, r[0], r[1], r[2], r[3])) {
                Module m = favMods.get(i);
                if (button == 1) open.accept(m);
                else VoidClient.get().modules().userToggle(m, false);
                return true;
            }
        }
        for (int i = 0; i < recentHit.size(); i++) {
            float[] r = recentHit.get(i);
            if (inBox(mx, my, r[0], r[1], r[2], r[3])) {
                open.accept(recentMods.get(i));
                ModuleManager.playClick();
                return true;
            }
        }
        float qy = y + 28 + 44 + 70 + 8 + 94;
        if (my >= qy && my < qy + 16) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mx >= x && mx < x + 110) {
                mc.setScreen(new voidclient.gui.screens.HudEditorScreen(mc.currentScreen));
            } else if (mx >= x + 116 && mx < x + 206) {
                VoidClient.get().config().saveAll();
                Notifications.success("Profile saved", VoidClient.get().profiles().active());
            } else if (mx >= x + 212 && mx < x + 322) {
                VoidClient.get().session().resetSession();
                Notifications.info("Session", "Statistics reset");
            } else return false;
            ModuleManager.playClick();
            return true;
        }
        return false;
    }

}
