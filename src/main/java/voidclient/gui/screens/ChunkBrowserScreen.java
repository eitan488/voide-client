package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import voidclient.VoidClient;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.modules.basehunting.ChunkIntelligence;
import voidclient.modules.basehunting.ChunkStats;
import voidclient.modules.basehunting.Component;
import voidclient.modules.basehunting.InspectionStore;
import voidclient.modules.world.WaypointStore;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Chunk Browser: search coordinates, browse/filter inspection history, compare two chunks,
 * bookmark, label, annotate, map preview and CSV/TXT report export.
 */
public class ChunkBrowserScreen extends VoidScreen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final TextField search = new TextField("Filter: label, note or x,z ...", 64);
    private final TextField goTo = new TextField("Chunk x z", 24);
    private final TextField label = new TextField("Label", 32);
    private final TextField notes = new TextField("Notes", 200);
    private String sort = "Total";
    private boolean bookmarksOnly;
    private InspectionStore.Entry selected, compare;
    private List<InspectionStore.Entry> shown = new ArrayList<>();
    private float scroll;

    public ChunkBrowserScreen(Screen parent) {
        super(Text.literal("Chunk Browser"), parent, 520, 300);
        search.onChange(s -> rebuild());
        goTo.onSubmit(this::jump);
        label.onSubmit(s -> {
            if (selected != null) {
                selected.label = s;
                InspectionStore.markDirty();
            }
        });
        notes.onSubmit(s -> {
            if (selected != null) {
                selected.notes = s;
                InspectionStore.markDirty();
            }
        });
    }

    private String dim() { return client.world == null ? "" : client.world.getRegistryKey().getValue().toString(); }

    @Override
    protected void layout() { rebuild(); }

    private void rebuild() {
        String q = search.getText().toLowerCase(Locale.ROOT).trim();
        shown = new ArrayList<>();
        for (InspectionStore.Entry e : InspectionStore.entries()) {
            if (bookmarksOnly && !e.bookmarked) continue;
            if (!q.isEmpty()) {
                String hay = (e.label + " " + e.notes + " " + e.cx + "," + e.cz + " " + e.cx + " " + e.cz).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }
            shown.add(e);
        }
        Comparator<InspectionStore.Entry> cmp = switch (sort) {
            case "Date" -> Comparator.comparingLong((InspectionStore.Entry e) -> -e.lastInspected);
            case "Distance" -> Comparator.comparingDouble(this::distance);
            default -> Comparator.comparingInt((InspectionStore.Entry e) -> -e.total());
        };
        shown.sort(cmp);
    }

    private double distance(InspectionStore.Entry e) {
        if (client.player == null) return 0;
        return Math.hypot(e.cx * 16 + 8 - client.player.getX(), e.cz * 16 + 8 - client.player.getZ());
    }

    private void jump(String s) {
        String[] p = s.trim().split("[ ,;]+");
        if (p.length < 2) return;
        try {
            int cx = Integer.parseInt(p[0]), cz = Integer.parseInt(p[1]);
            InspectionStore.Entry e = InspectionStore.find(cx, cz, dim());
            if (e == null) {
                ChunkIntelligence ci = ChunkIntelligence.INSTANCE;
                ChunkStats live = ci == null ? null : ci.scanner().get(cx, cz);
                if (live != null) {
                    InspectionStore.record(live);
                    e = InspectionStore.find(cx, cz, dim());
                }
            }
            if (e == null) Notifications.warning("Chunk Browser", "Chunk " + cx + " " + cz + " has not been inspected (only loaded chunks can be)");
            else select(e);
            rebuild();
        } catch (NumberFormatException ex) {
            Notifications.error("Chunk Browser", "Enter two integers: chunk x z");
        }
    }

    private void select(InspectionStore.Entry e) {
        selected = e;
        label.setText(e.label);
        notes.setText(e.notes);
    }

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        float x = 14, y = 12, w = vw - 28, h = vh - 24;
        Render2D.glow(ctx, x, y, w, h, 10, 0.8f, ThemeManager.accent(0));
        Render2D.roundRect(ctx, x, y, w, h, 10, ColorUtil.withAlpha(t.background(), 240));
        Render2D.roundRectOutlineGradient(ctx, x, y, w, h, 10, 1f, ThemeManager.accent(0), ThemeManager.accent(0.5f));
        UI.bold(ctx, "CHUNK BROWSER", x + 12, y + 9, ThemeManager.accent(0));
        UI.small(ctx, InspectionStore.entries().size() + " inspected chunks in this world  •  client-visible data only", x + 12, y + 21, t.textDim());

        float lx = x + 10, ly = y + 34, lw = w * 0.46f;
        search.bounds(lx, ly, lw - 120, 15);
        search.render(ctx, mx, my, delta);
        String[] sorts = {"Total", "Date", "Distance"};
        float sx = lx + lw - 116;
        for (String s : sorts) {
            float cw = UI.chipWidth(s);
            UI.chip(ctx, sx, ly + 1.5f, cw, 12, s, sort.equals(s), mx >= sx && mx < sx + cw && my >= ly && my < ly + 15);
            sx += cw + 2;
        }
        float rowY = ly + 20;
        float listH = h - (rowY - y) - 46;
        UI.chip(ctx, lx, rowY + listH + 4, 70, 12, "Bookmarks", bookmarksOnly, mx >= lx && mx < lx + 70 && my >= rowY + listH + 4 && my < rowY + listH + 16);
        Render2D.scissor(ctx, lx, rowY, lw, listH);
        float ry = rowY - scroll;
        for (InspectionStore.Entry e : shown) {
            if (ry + 20 >= rowY && ry <= rowY + listH) {
                boolean hover = mx >= lx && mx < lx + lw && my >= ry && my < ry + 19 && my >= rowY && my < rowY + listH;
                int bg = e == selected ? ColorUtil.withAlpha(ThemeManager.accent(0), 70) : e == compare ? ColorUtil.withAlpha(ThemeManager.accent(0.5f), 60) : hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surface(), 160);
                Render2D.roundRect(ctx, lx, ry, lw, 18, 4, bg);
                Render2D.roundRect(ctx, lx + 3, ry + 3, 3, 12, 1.5f, e.heatColor());
                String title = (e.bookmarked ? "★ " : "") + (e.label.isEmpty() ? "Chunk " + e.cx + " / " + e.cz : e.label + "  (" + e.cx + "/" + e.cz + ")");
                UI.text(ctx, UI.trim(title, lw - 110), lx + 10, ry + 5, t.text());
                String right = e.total() + " parts  " + Math.round(distance(e)) + "m";
                UI.small(ctx, right, lx + lw - 6 - UI.smallWidth(right), ry + 6, t.textDim());
            }
            ry += 20;
        }
        Render2D.endScissor(ctx);
        if (shown.isEmpty()) {
            var ci = voidclient.modules.basehunting.ChunkIntelligence.INSTANCE;
            if (ci == null || !ci.isEnabled()) {
                UI.small(ctx, "No inspected chunks yet - enable Chunk Intelligence and explore.", lx + 4, rowY + 6, t.textDim());
            } else {
                UI.small(ctx, ci.scanner().results().size() + " chunks scanned, none at or above the activity threshold (" + ci.threshold() + ").", lx + 4, rowY + 6, t.textDim());
                UI.small(ctx, "Only chunks that meet the threshold are kept in history (Chunk Intelligence > Minimum Count).", lx + 4, rowY + 18, t.textDim());
            }
        }

        float dx = lx + lw + 12, dw = w - (dx - x) - 10;
        goTo.bounds(dx, ly, dw - 50, 15);
        goTo.render(ctx, mx, my, delta);
        UI.button(ctx, dx + dw - 46, ly, 46, 15, "Go", mx >= dx + dw - 46 && mx < dx + dw && my >= ly && my < ly + 15, true);
        float dy = ly + 20;
        if (selected == null) {
            UI.small(ctx, "Select a chunk to see its statistics. Shift-click a second chunk to compare.", dx, dy + 4, t.textDim());
        } else {
            renderDetails(ctx, mx, my, delta, dx, dy, dw, t);
        }
        String[] actions = {"Bookmark + Waypoint", "Export CSV", "Export Report", "Open World Map", "Close"};
        float bw = (w - 20 - 4 * 4) / actions.length, by = y + h - 22;
        for (int i = 0; i < actions.length; i++) {
            float bx = x + 10 + i * (bw + 4);
            UI.button(ctx, bx, by, bw, 15, actions[i], mx >= bx && mx < bx + bw && my >= by && my < by + 15, i == 0);
        }
    }

    private void renderDetails(DrawContext ctx, double mx, double my, float delta, float dx, float dy, float dw, Theme t) {
        InspectionStore.Entry e = selected;
        UI.bold(ctx, "CHUNK " + e.cx + " / " + e.cz + (compare != null ? "   vs   " + compare.cx + " / " + compare.cz : ""), dx, dy, 0xFFFFFFFF);
        UI.small(ctx, "Blocks " + (e.cx * 16) + ".." + (e.cx * 16 + 15) + ", " + (e.cz * 16) + ".." + (e.cz * 16 + 15) + "   inspected "
                + DATE.format(Instant.ofEpochMilli(e.lastInspected).atZone(ZoneId.systemDefault())), dx, dy + 11, t.textDim());
        float ty = dy + 22;
        for (Component.Group g : Component.Group.values()) {
            int a = e.group(g);
            String v = compare == null ? String.valueOf(a) : a + "  |  " + compare.group(g) + "  (" + signed(a - compare.group(g)) + ")";
            UI.text(ctx, g.label, dx, ty, g.color);
            UI.text(ctx, v, dx + dw * 0.45f, ty, t.text());
            ty += 10;
            for (Component c : Component.values()) {
                if (c.group != g) continue;
                int va = e.counts.getOrDefault(c, 0), vb = compare == null ? 0 : compare.counts.getOrDefault(c, 0);
                if (va == 0 && vb == 0) continue;
                UI.small(ctx, "  " + c.label, dx, ty, t.textDim());
                UI.small(ctx, compare == null ? String.valueOf(va) : va + "  |  " + vb, dx + dw * 0.45f, ty, t.text());
                ty += 8;
            }
            if (ty > dy + 150) break;
        }
        float fy = Math.max(ty + 4, dy + 120);
        label.bounds(dx, fy, dw * 0.48f, 14);
        label.render(ctx, mx, my, delta);
        notes.bounds(dx + dw * 0.5f, fy, dw * 0.5f, 14);
        notes.render(ctx, mx, my, delta);
        UI.small(ctx, "Enter saves label / notes", dx, fy + 16, t.textDim());
        // mini map of inspected chunks around the selection
        float cell = 6, mapR = 5;
        float mapX = dx + dw - (mapR * 2 + 1) * cell, mapY = dy;
        for (int ox = (int) -mapR; ox <= mapR; ox++) for (int oz = (int) -mapR; oz <= mapR; oz++) {
            InspectionStore.Entry n = InspectionStore.find(e.cx + ox, e.cz + oz, e.dimension);
            int c = n == null ? 0x30FFFFFF : n.heatColor();
            Render2D.rect(ctx, mapX + (ox + mapR) * cell, mapY + (oz + mapR) * cell, cell - 1, cell - 1, c);
        }
        Render2D.outline(ctx, mapX + mapR * cell - 1, mapY + mapR * cell - 1, cell + 1, cell + 1, 1, 0xFFFFFFFF);
    }

    private static String signed(int v) { return v > 0 ? "+" + v : String.valueOf(v); }

    private Path reportsDir() throws IOException {
        Path p = VoidClient.get().config().dir().resolve("reports");
        Files.createDirectories(p);
        return p;
    }

    private void exportCsv() {
        StringBuilder sb = new StringBuilder("chunk_x,chunk_z,dimension,label,notes,bookmarked,first_seen,last_inspected,total");
        for (Component c : Component.values()) sb.append(',').append(c.name().toLowerCase(Locale.ROOT));
        sb.append('\n');
        for (InspectionStore.Entry e : InspectionStore.entries()) {
            sb.append(e.cx).append(',').append(e.cz).append(',').append(e.dimension).append(',').append(csv(e.label)).append(',').append(csv(e.notes)).append(',')
                    .append(e.bookmarked).append(',').append(e.firstSeen).append(',').append(e.lastInspected).append(',').append(e.total());
            for (Component c : Component.values()) sb.append(',').append(e.counts.getOrDefault(c, 0));
            sb.append('\n');
        }
        write("chunks-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv", sb.toString());
    }

    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private void exportReport() {
        if (selected == null) {
            Notifications.warning("Report", "Select a chunk first");
            return;
        }
        InspectionStore.Entry e = selected;
        StringBuilder sb = new StringBuilder();
        sb.append("VOID CLIENT - CHUNK INSPECTION REPORT\n");
        sb.append("Chunk: ").append(e.cx).append(" / ").append(e.cz).append("  (").append(e.dimension).append(")\n");
        sb.append("Blocks: x ").append(e.cx * 16).append("..").append(e.cx * 16 + 15).append(", z ").append(e.cz * 16).append("..").append(e.cz * 16 + 15).append('\n');
        sb.append("Inspected: ").append(DATE.format(Instant.ofEpochMilli(e.lastInspected).atZone(ZoneId.systemDefault()))).append('\n');
        sb.append("Label: ").append(e.label).append("\nNotes: ").append(e.notes).append("\n\n");
        for (Component.Group g : Component.Group.values()) {
            sb.append(g.label.toUpperCase(Locale.ROOT)).append(": ").append(e.group(g)).append('\n');
            for (Component c : Component.values()) if (c.group == g && e.counts.getOrDefault(c, 0) > 0) sb.append("  - ").append(c.label).append(": ").append(e.counts.get(c)).append('\n');
        }
        sb.append("\nCounts reflect only the chunk data the server sent to this client at inspection time.\n");
        write("chunk-" + e.cx + "_" + e.cz + ".txt", sb.toString());
    }

    private void write(String name, String content) {
        try {
            Path f = reportsDir().resolve(name);
            Files.writeString(f, content, StandardCharsets.UTF_8);
            Notifications.success("Exported", "reports/" + name);
            Util.getOperatingSystem().open(reportsDir().toFile());
        } catch (IOException ex) {
            Notifications.error("Export failed", ex.getMessage());
        }
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (search.mouseClicked(mx, my, button) | goTo.mouseClicked(mx, my, button) | label.mouseClicked(mx, my, button) | notes.mouseClicked(mx, my, button)) return true;
        float x = 14, y = 12, w = vw - 28, h = vh - 24;
        float lx = x + 10, ly = y + 34, lw = w * 0.46f;
        String[] sorts = {"Total", "Date", "Distance"};
        float sx = lx + lw - 116;
        for (String s : sorts) {
            float cw = UI.chipWidth(s);
            if (mx >= sx && mx < sx + cw && my >= ly && my < ly + 15) {
                sort = s;
                rebuild();
                ModuleManager.playClick();
                return true;
            }
            sx += cw + 2;
        }
        float rowY = ly + 20, listH = h - (rowY - y) - 46;
        if (mx >= lx && mx < lx + 70 && my >= rowY + listH + 4 && my < rowY + listH + 16) {
            bookmarksOnly = !bookmarksOnly;
            rebuild();
            return true;
        }
        if (mx >= lx && mx < lx + lw && my >= rowY && my < rowY + listH) {
            int idx = (int) ((my - rowY + scroll) / 20);
            if (idx >= 0 && idx < shown.size()) {
                InspectionStore.Entry e = shown.get(idx);
                if (net.minecraft.client.util.InputUtil.isKeyPressed(client.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) && selected != null && e != selected) compare = e;
                else {
                    compare = null;
                    select(e);
                }
                ModuleManager.playClick();
            }
            return true;
        }
        float dx = lx + lw + 12, dw = w - (dx - x) - 10;
        if (mx >= dx + dw - 46 && mx < dx + dw && my >= ly && my < ly + 15) {
            jump(goTo.getText());
            return true;
        }
        String[] actions = {"Bookmark + Waypoint", "Export CSV", "Export Report", "Open World Map", "Close"};
        float bw = (w - 20 - 4 * 4) / actions.length, by = y + h - 22;
        if (my >= by && my < by + 15) {
            int i = (int) ((mx - x - 10) / (bw + 4));
            ModuleManager.playClick();
            switch (i) {
                case 0 -> {
                    if (selected != null) {
                        selected.bookmarked = true;
                        InspectionStore.markDirty();
                        int yy = client.player == null ? 64 : client.player.getBlockY();
                        WaypointStore.add(selected.label.isEmpty() ? "Chunk " + selected.cx + "/" + selected.cz : selected.label,
                                selected.cx * 16 + 8, yy, selected.cz * 16 + 8, selected.heatColor(), "Chunk");
                        Notifications.success("Saved location", "Bookmarked and waypoint created");
                    }
                }
                case 1 -> exportCsv();
                case 2 -> exportReport();
                case 3 -> client.setScreen(new WorldMapScreen(this));
                case 4 -> close();
                default -> {
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        scroll = Math.max(0, Math.min(Math.max(0, shown.size() * 20 - 100), scroll - (float) amount * 20));
        return true;
    }

    private TextField focused() {
        for (TextField f : new TextField[]{search, goTo, label, notes}) if (f.capturesKeyboard()) return f;
        return null;
    }

    @Override
    protected boolean onKey(int key, int scancode, int mods) {
        TextField f = focused();
        return f != null && f.keyPressed(key, scancode, mods);
    }

    @Override
    protected boolean onChar(String chars) {
        TextField f = focused();
        return f != null && f.charTyped(chars);
    }

    @Override
    public void removed() { InspectionStore.save(); }
}
