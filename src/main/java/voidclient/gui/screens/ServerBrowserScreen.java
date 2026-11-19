package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.SharedConstants;
import net.minecraft.text.Text;
import voidclient.VoidClient;
import voidclient.core.ModuleManager;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Void server browser: live pinging, MOTD, player counts, latency bars and management. */
public class ServerBrowserScreen extends VoidScreen {
    private static final ExecutorService PINGER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Void Server Pinger");
        t.setDaemon(true);
        return t;
    });

    private ServerList servers;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    private final Set<ServerInfo> pinged = ConcurrentHashMap.newKeySet();
    private int selected = -1;
    private float scroll;
    private long lastClick;
    private float lx, ly, lw, lh;

    public ServerBrowserScreen(Screen parent) {
        super(Text.literal("Servers"), parent, 420, 260);
    }

    @Override
    protected void layout() {
        servers = new ServerList(client);
        servers.loadFile();
        lw = Math.min(520, vw - 40);
        lx = (vw - lw) / 2f;
        ly = 46;
        lh = vh - ly - 48;
    }

    @Override
    public void tick() { pinger.tick(); }

    @Override
    public void removed() { pinger.cancel(); }

    private void ping(ServerInfo info) {
        if (!pinged.add(info)) return;
        info.setStatus(ServerInfo.Status.PINGING);
        info.label = Text.literal("Pinging...");
        PINGER.submit(() -> {
            try {
                pinger.add(info, () -> client.execute(servers::saveFile), () -> info.setStatus(
                        info.protocolVersion == SharedConstants.getGameVersion().protocolVersion() ? ServerInfo.Status.SUCCESSFUL : ServerInfo.Status.INCOMPATIBLE),
                        NetworkingBackend.remote(client.options.shouldUseNativeTransport()));
            } catch (Exception e) {
                info.setStatus(ServerInfo.Status.UNREACHABLE);
                info.label = Text.literal("Can't reach server");
            }
        });
    }

    private void refresh() {
        pinged.clear();
        servers.loadFile();
    }

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        UI.bold(ctx, "Multiplayer", lx, 16, 0xFFFFFFFF);
        UI.small(ctx, servers.size() + " saved servers  •  double-click to join", lx, 28, t.textDim());
        String vanilla = "Vanilla List";
        float vwid = UI.width(vanilla) + 14;
        UI.button(ctx, lx + lw - vwid, 14, vwid, 15, vanilla, mx >= lx + lw - vwid && mx < lx + lw && my >= 14 && my < 29, false);

        Render2D.roundRect(ctx, lx - 4, ly - 4, lw + 8, lh + 8, 8, ColorUtil.withAlpha(t.background(), 200));
        Render2D.roundRectOutline(ctx, lx - 4, ly - 4, lw + 8, lh + 8, 8, 0.8f, ColorUtil.withAlpha(ThemeManager.accent(0), 120));
        Render2D.scissor(ctx, lx, ly, lw, lh);
        float rowH = 36;
        float maxScroll = Math.max(0, servers.size() * rowH - lh);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo s = servers.get(i);
            float ry = ly + i * rowH - scroll;
            if (ry + rowH < ly || ry > ly + lh) continue;
            ping(s);
            boolean hover = mx >= lx && mx < lx + lw && my >= ry && my < ry + rowH - 2 && my >= ly && my < ly + lh;
            boolean sel = i == selected;
            Render2D.roundRect(ctx, lx, ry, lw, rowH - 3, 6, sel ? ColorUtil.withAlpha(ThemeManager.accent(0), 55) : hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surface(), 190));
            if (sel) Render2D.roundRectOutline(ctx, lx, ry, lw, rowH - 3, 6, 1f, ThemeManager.accent(0));
            Render2D.roundRect(ctx, lx + 5, ry + 4, 25, 25, 4, ColorUtil.withAlpha(ThemeManager.accent(i * 0.1f), 120));
            UI.bold(ctx, s.name.isEmpty() ? "?" : s.name.substring(0, 1).toUpperCase(), lx + 14, ry + 13, 0xFFFFFFFF);
            UI.bold(ctx, UI.trim(s.name, lw - 130), lx + 36, ry + 5, 0xFFFFFFFF);
            String motd = s.label == null ? "" : s.label.getString().replace('\n', ' ');
            UI.small(ctx, UI.trim(motd, (lw - 130) / 0.75f), lx + 36, ry + 17, t.textDim());
            UI.small(ctx, s.address, lx + 36, ry + 25, ColorUtil.withAlpha(t.textDim(), 140));
            String players = s.playerCountLabel == null ? "" : s.playerCountLabel.getString();
            UI.small(ctx, players, lx + lw - 10 - UI.smallWidth(players), ry + 18, t.text());
            ServerInfo.Status st = s.getStatus();
            int bars = st != ServerInfo.Status.SUCCESSFUL ? 0 : s.ping < 150 ? 5 : s.ping < 300 ? 4 : s.ping < 600 ? 3 : s.ping < 1000 ? 2 : 1;
            int col = st == ServerInfo.Status.UNREACHABLE ? t.danger() : st == ServerInfo.Status.INCOMPATIBLE ? t.warning() : t.success();
            for (int b = 0; b < 5; b++) {
                float bh = 2 + b * 1.6f;
                Render2D.rect(ctx, lx + lw - 34 + b * 4, ry + 13 - bh, 3, bh, b < bars ? col : ColorUtil.withAlpha(t.textDim(), 80));
            }
            String ms = st == ServerInfo.Status.SUCCESSFUL ? s.ping + "ms" : st == ServerInfo.Status.PINGING || st == ServerInfo.Status.INITIAL ? "..." : st == ServerInfo.Status.INCOMPATIBLE ? "version" : "offline";
            UI.small(ctx, ms, lx + lw - 40 - UI.smallWidth(ms), ry + 6, col);
        }
        Render2D.endScissor(ctx);
        if (servers.size() == 0) UI.centered(ctx, "No servers saved yet - Add one below.", lx + lw / 2f, ly + 30, t.textDim());

        String[] labels = {"Join", "Direct", "Add", "Edit", "Delete", "Refresh", "Back"};
        float bw = (lw - 6 * 4) / labels.length, by = vh - 36;
        for (int i = 0; i < labels.length; i++) {
            float bx = lx + i * (bw + 4);
            boolean needsSel = i == 0 || i == 3 || i == 4;
            boolean enabled = !needsSel || selected >= 0;
            UI.button(ctx, bx, by, bw, 18, labels[i], enabled && mx >= bx && mx < bx + bw && my >= by && my < by + 18, i == 0 && enabled);
        }
    }

    private void join(ServerInfo info) {
        ConnectScreen.connect(this, client, ServerAddress.parse(info.address), info, false, null);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        String vanilla = "Vanilla List";
        float vwid = UI.width(vanilla) + 14;
        if (mx >= lx + lw - vwid && mx < lx + lw && my >= 14 && my < 29) {
            ModuleManager.playClick();
            ScreenReplacer.bypassNext();
            client.setScreen(new MultiplayerScreen(parent));
            return true;
        }
        if (mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
            int idx = (int) ((my - ly + scroll) / 36);
            if (idx >= 0 && idx < servers.size()) {
                boolean dbl = idx == selected && System.currentTimeMillis() - lastClick < 300;
                selected = idx;
                lastClick = System.currentTimeMillis();
                if (dbl) join(servers.get(idx));
                ModuleManager.playClick();
                return true;
            }
        }
        String[] labels = {"Join", "Direct", "Add", "Edit", "Delete", "Refresh", "Back"};
        float bw = (lw - 6 * 4) / labels.length, by = vh - 36;
        if (my >= by && my < by + 18) {
            int i = (int) ((mx - lx) / (bw + 4));
            if (i < 0 || i >= labels.length) return false;
            ModuleManager.playClick();
            switch (i) {
                case 0 -> {
                    if (selected >= 0) join(servers.get(selected));
                }
                case 1 -> {
                    ServerInfo info = new ServerInfo(Text.translatable("selectServer.defaultName").getString(), "", ServerInfo.ServerType.OTHER);
                    client.setScreen(new DirectConnectScreen(this, ok -> {
                        if (ok) join(info);
                        else client.setScreen(this);
                    }, info));
                }
                case 2 -> {
                    ServerInfo info = new ServerInfo(Text.translatable("selectServer.defaultName").getString(), "", ServerInfo.ServerType.OTHER);
                    client.setScreen(new AddServerScreen(this, Text.translatable("manageServer.add.title"), ok -> {
                        if (ok) {
                            servers.add(info, false);
                            servers.saveFile();
                        }
                        client.setScreen(this);
                    }, info));
                }
                case 3 -> {
                    if (selected < 0) return true;
                    ServerInfo original = servers.get(selected);
                    ServerInfo edit = new ServerInfo(original.name, original.address, ServerInfo.ServerType.OTHER);
                    edit.copyWithSettingsFrom(original);
                    client.setScreen(new AddServerScreen(this, Text.translatable("manageServer.edit.title"), ok -> {
                        if (ok) {
                            original.copyWithSettingsFrom(edit);
                            servers.saveFile();
                            pinged.remove(original);
                        }
                        client.setScreen(this);
                    }, edit));
                }
                case 4 -> {
                    if (selected < 0) return true;
                    servers.remove(servers.get(selected));
                    servers.saveFile();
                    selected = -1;
                }
                case 5 -> refresh();
                case 6 -> close();
                default -> {
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        scroll -= (float) amount * 24;
        return true;
    }
}
