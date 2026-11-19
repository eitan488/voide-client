package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.modules.basehunting.InspectionStore;
import voidclient.modules.world.MapCache;
import voidclient.modules.world.Waypoint;
import voidclient.modules.world.WaypointStore;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.List;

/** Explored world map with pan/zoom, waypoints, inspected chunks and a waypoint manager. */
public class WorldMapScreen extends VoidScreen {
    private double centerX, centerZ;
    private float zoom = 1f; // screen units per block
    private boolean dragging;
    private boolean showInspected = true, showGrid = true;
    private Waypoint selected;
    private final TextField rename = new TextField("Rename waypoint...", 32);
    private float listScroll;
    private static final float LIST_W = 150;

    public WorldMapScreen(Screen parent) {
        super(Text.literal("World Map"), parent, 400, 240);
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            centerX = mc.player.getX();
            centerZ = mc.player.getZ();
        }
        rename.onSubmit(s -> {
            if (selected != null && !s.isBlank()) {
                selected.name = s;
                WaypointStore.save();
            }
        });
    }

    @Override
    protected void layout() {
        if (client.player != null && centerX == 0 && centerZ == 0) {
            centerX = client.player.getX();
            centerZ = client.player.getZ();
        }
    }

    private float sx(double wx) { return (float) ((wx - centerX) * zoom + (vw - LIST_W) / 2f); }
    private float sz(double wz) { return (float) ((wz - centerZ) * zoom + vh / 2f); }
    private double wx(double sx) { return (sx - (vw - LIST_W) / 2f) / zoom + centerX; }
    private double wz(double sy) { return (sy - vh / 2f) / zoom + centerZ; }

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        float mapW = vw - LIST_W;
        Render2D.rect(ctx, 0, 0, mapW, vh, 0xFF06060C);
        Render2D.scissor(ctx, 0, 0, mapW, vh);
        int minRx = (int) Math.floor(wx(0) / MapCache.REGION), maxRx = (int) Math.floor(wx(mapW) / MapCache.REGION);
        int minRz = (int) Math.floor(wz(0) / MapCache.REGION), maxRz = (int) Math.floor(wz(vh) / MapCache.REGION);
        if ((maxRx - minRx + 1) * (maxRz - minRz + 1) <= 64) {
            for (int rx = minRx; rx <= maxRx; rx++) {
                for (int rz = minRz; rz <= maxRz; rz++) {
                    MapCache.Region r = MapCache.region(rx, rz, true);
                    if (r == null) continue;
                    float x = sx(rx * (double) MapCache.REGION), z = sz(rz * (double) MapCache.REGION);
                    Render2D.texture(ctx, r.id(), x, z, MapCache.REGION * zoom, MapCache.REGION * zoom, 0xFFFFFFFF);
                }
            }
        }
        if (showGrid && zoom >= 0.5f) {
            int c = ColorUtil.withAlpha(t.text(), 18);
            double step = 16;
            for (double x = Math.floor(wx(0) / step) * step; x < wx(mapW); x += step) Render2D.rect(ctx, sx(x), 0, 0.5f, vh, c);
            for (double z = Math.floor(wz(0) / step) * step; z < wz(vh); z += step) Render2D.rect(ctx, 0, sz(z), mapW, 0.5f, c);
        }
        if (showInspected) {
            for (InspectionStore.Entry e : InspectionStore.entries()) {
                if (!e.dimension.equals(WaypointStore.currentDimension())) continue;
                float x = sx(e.cx * 16.0), z = sz(e.cz * 16.0);
                int c = ColorUtil.withAlpha(e.heatColor(), 90);
                Render2D.rect(ctx, x, z, 16 * zoom, 16 * zoom, c);
                Render2D.outline(ctx, x, z, 16 * zoom, 16 * zoom, 0.6f, ColorUtil.withAlpha(e.heatColor(), 220));
            }
        }
        for (Waypoint w : WaypointStore.inCurrentDimension()) {
            float x = sx(w.x + 0.5), z = sz(w.z + 0.5);
            Render2D.circle(ctx, x, z, w == selected ? 4.5f : 3.5f, 0xFF000000);
            Render2D.circle(ctx, x, z, w == selected ? 3.5f : 2.5f, w.color | 0xFF000000);
            UI.small(ctx, w.name, x + 5, z - 3, 0xFFFFFFFF);
        }
        if (client.player != null) {
            float x = sx(client.player.getX()), z = sz(client.player.getZ());
            double a = Math.toRadians(client.player.getYaw() + 180);
            float fx = (float) -Math.sin(a), fz = (float) Math.cos(a);
            float bx = -fz, bz = fx;
            Render2D.triangle(ctx, x - fx * 6, z - fz * 6, x + fx * 4 + bx * 4, z + fz * 4 + bz * 4, x + fx * 4 - bx * 4, z + fz * 4 - bz * 4, 0xFFFFFFFF);
        }
        Render2D.endScissor(ctx);
        String coords = String.format("X %d  Z %d   zoom %.2fx   drag: pan  scroll: zoom  right-click: add waypoint  G grid  I inspected  Space recenter",
                (int) Math.floor(wx(mx)), (int) Math.floor(wz(my)), zoom);
        Render2D.roundRect(ctx, 6, vh - 16, UI.smallWidth(coords) + 10, 12, 4, 0xC0000000);
        UI.small(ctx, coords, 11, vh - 13, t.text());
        renderList(ctx, mx, my, delta, mapW);
    }

    private void renderList(DrawContext ctx, double mx, double my, float delta, float x) {
        Theme t = ThemeManager.get();
        Render2D.rect(ctx, x, 0, LIST_W, vh, ColorUtil.withAlpha(t.background(), 240));
        Render2D.rect(ctx, x, 0, 0.8f, vh, ThemeManager.accent(0));
        UI.bold(ctx, "Waypoints", x + 8, 8, 0xFFFFFFFF);
        List<Waypoint> list = WaypointStore.inCurrentDimension();
        float y = 24 - listScroll;
        Render2D.scissor(ctx, x, 22, LIST_W, vh - 110);
        for (Waypoint w : list) {
            boolean hover = mx >= x && my >= y && my < y + 16 && my >= 22 && my < vh - 88;
            if (w == selected || hover) Render2D.roundRect(ctx, x + 4, y, LIST_W - 8, 15, 4, w == selected ? ColorUtil.withAlpha(ThemeManager.accent(0), 70) : t.surfaceHover());
            Render2D.circle(ctx, x + 11, y + 7.5f, 3, w.color | 0xFF000000);
            UI.text(ctx, UI.trim(w.name, LIST_W - 50), x + 18, y + 4, w.visible ? t.text() : t.textDim());
            UI.small(ctx, w.type, x + LIST_W - 8 - UI.smallWidth(w.type), y + 5, t.textDim());
            y += 17;
        }
        Render2D.endScissor(ctx);
        float by = vh - 84;
        if (selected != null) {
            UI.small(ctx, selected.x + " " + selected.y + " " + selected.z, x + 8, by - 2, t.textDim());
            rename.bounds(x + 6, by + 8, LIST_W - 12, 14);
            rename.render(ctx, mx, my, delta);
            String[] b = {"Go To", selected.visible ? "Hide" : "Show", "Delete"};
            float bw = (LIST_W - 12 - 8) / 3f;
            for (int i = 0; i < 3; i++) UI.button(ctx, x + 6 + i * (bw + 4), by + 26, bw, 14, b[i], mx >= x + 6 + i * (bw + 4) && mx < x + 6 + i * (bw + 4) + bw && my >= by + 26 && my < by + 40, i == 0);
        }
        UI.button(ctx, x + 6, vh - 22, LIST_W - 12, 15, "Add At My Position", mx >= x + 6 && my >= vh - 22 && my < vh - 7, false);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        float mapW = vw - LIST_W;
        if (mx >= mapW) {
            if (rename.mouseClicked(mx, my, button)) return true;
            List<Waypoint> list = WaypointStore.inCurrentDimension();
            if (my >= 22 && my < vh - 88) {
                int idx = (int) ((my - 24 + listScroll) / 17);
                if (idx >= 0 && idx < list.size()) {
                    selected = list.get(idx);
                    rename.setText(selected.name);
                    ModuleManager.playClick();
                }
                return true;
            }
            float by = vh - 84;
            if (selected != null && my >= by + 26 && my < by + 40) {
                float bw = (LIST_W - 12 - 8) / 3f;
                int i = (int) ((mx - mapW - 6) / (bw + 4));
                switch (i) {
                    case 0 -> {
                        centerX = selected.x;
                        centerZ = selected.z;
                    }
                    case 1 -> {
                        selected.visible = !selected.visible;
                        WaypointStore.save();
                    }
                    case 2 -> {
                        WaypointStore.remove(selected);
                        selected = null;
                    }
                    default -> {
                    }
                }
                ModuleManager.playClick();
                return true;
            }
            if (my >= vh - 22 && client.player != null) {
                var p = client.player.getBlockPos();
                selected = WaypointStore.add("Waypoint " + (WaypointStore.all().size() + 1), p.getX(), p.getY(), p.getZ(), ThemeManager.accent(0), "Manual");
                if (selected != null) rename.setText(selected.name);
                ModuleManager.playClick();
            }
            return true;
        }
        if (button == 1) {
            int x = (int) Math.floor(wx(mx)), z = (int) Math.floor(wz(my));
            int y = client.world != null ? voidclient.modules.world.MapColors.surfaceY(client.world, x, z) : 64;
            if (y == Integer.MIN_VALUE) y = client.player != null ? client.player.getBlockY() : 64;
            selected = WaypointStore.add("Waypoint " + (WaypointStore.all().size() + 1), x, y, z, ThemeManager.accent(0), "Map");
            if (selected != null) {
                rename.setText(selected.name);
                Notifications.success("Waypoint added", x + " " + y + " " + z);
            }
            return true;
        }
        dragging = true;
        return true;
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return false;
        centerX -= dx / zoom;
        centerZ -= dy / zoom;
        return true;
    }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        dragging = false;
        return true;
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        if (mx >= vw - LIST_W) {
            listScroll = Math.max(0, listScroll - (float) amount * 17);
            return true;
        }
        double bx = wx(mx), bz = wz(my);
        zoom = (float) Math.max(0.0625, Math.min(16, zoom * (amount > 0 ? 1.25 : 0.8)));
        centerX = bx - (mx - (vw - LIST_W) / 2f) / zoom;
        centerZ = bz - (my - vh / 2f) / zoom;
        return true;
    }

    @Override
    protected boolean onKey(int key, int scancode, int mods) {
        if (rename.capturesKeyboard()) return rename.keyPressed(key, scancode, mods);
        switch (key) {
            case GLFW.GLFW_KEY_G -> showGrid = !showGrid;
            case GLFW.GLFW_KEY_I -> showInspected = !showInspected;
            case GLFW.GLFW_KEY_SPACE -> {
                if (client.player != null) {
                    centerX = client.player.getX();
                    centerZ = client.player.getZ();
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean onChar(String chars) { return rename.capturesKeyboard() && rename.charTyped(chars); }

    @Override
    public boolean shouldPause() { return false; }
}
