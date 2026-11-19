package voidclient.gui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Screenshot gallery: thumbnails, open, rename, delete, copy path, open folder. */
public class ScreenshotBrowserScreen extends VoidScreen {
    private final List<Path> files = new ArrayList<>();
    private final Map<Path, Identifier> loaded = new HashMap<>();
    private final Map<Path, int[]> sizes = new HashMap<>();
    private int page;
    private Path selected;
    private final TextField rename = new TextField("New name (without .png)", 64);
    private final String sort;
    private static final int COLS = 3, ROWS = 3;

    public ScreenshotBrowserScreen(Screen parent, String sort) {
        super(Text.literal("Screenshots"), parent, 420, 260);
        this.sort = sort;
    }

    private Path dir() { return client.runDirectory.toPath().resolve("screenshots"); }

    @Override
    protected void layout() { reload(); }

    private void reload() {
        files.clear();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.filter(p -> p.toString().toLowerCase().endsWith(".png")).forEach(files::add);
            }
        } catch (IOException e) {
            Notifications.error("Screenshots", e.getMessage());
        }
        Comparator<Path> byTime = Comparator.comparingLong(p -> p.toFile().lastModified());
        switch (sort) {
            case "Oldest" -> files.sort(byTime);
            case "Name" -> files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            default -> files.sort(byTime.reversed());
        }
        page = Math.max(0, Math.min(page, (files.size() - 1) / (COLS * ROWS)));
    }

    private Identifier thumb(Path p) {
        Identifier id = loaded.get(p);
        if (id != null) return id;
        if (loaded.size() >= COLS * ROWS * 2) releaseAll();
        try (InputStream in = Files.newInputStream(p)) {
            NativeImage img = NativeImage.read(in);
            sizes.put(p, new int[]{img.getWidth(), img.getHeight()});
            id = Identifier.of("voidclient", "dynamic/shot/" + Integer.toHexString(p.hashCode()));
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "Void screenshot", img));
            loaded.put(p, id);
            return id;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private void releaseAll() {
        for (Identifier id : loaded.values()) client.getTextureManager().destroyTexture(id);
        loaded.clear();
    }

    @Override
    public void removed() { releaseAll(); }

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        float gx = 20, gy = 40, gw = vw - 200, gh = vh - 70;
        UI.bold(ctx, "Screenshots", gx, 16, 0xFFFFFFFF);
        UI.small(ctx, files.size() + " files  •  page " + (page + 1) + "/" + Math.max(1, (files.size() + COLS * ROWS - 1) / (COLS * ROWS)), gx, 28, t.textDim());
        float cw = (gw - (COLS - 1) * 8) / COLS, ch = (gh - (ROWS - 1) * 8) / ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = page * COLS * ROWS + i;
            if (idx >= files.size()) break;
            Path p = files.get(idx);
            float x = gx + (i % COLS) * (cw + 8), y = gy + (i / COLS) * (ch + 8);
            boolean hover = mx >= x && mx < x + cw && my >= y && my < y + ch;
            Render2D.roundRect(ctx, x, y, cw, ch, 5, ColorUtil.withAlpha(t.surface(), 200));
            Identifier id = thumb(p);
            if (id != null) {
                int[] sz = sizes.get(p);
                float ar = sz[0] / (float) sz[1];
                float tw = cw - 6, th = tw / ar;
                if (th > ch - 16) {
                    th = ch - 16;
                    tw = th * ar;
                }
                Render2D.texture(ctx, id, x + (cw - tw) / 2f, y + 3, tw, th, 0xFFFFFFFF);
            }
            UI.small(ctx, UI.trim(p.getFileName().toString(), (cw - 6) / 0.75f), x + 3, y + ch - 10, t.textDim());
            Render2D.roundRectOutline(ctx, x, y, cw, ch, 5, p.equals(selected) ? 1.2f : 0.6f, p.equals(selected) ? ThemeManager.accent(0) : hover ? t.text() : t.stroke());
        }
        float px = vw - 170, pw = 150;
        float by = 40;
        String[] actions = {"Open", "Copy Path", "Delete", "Open Folder", "Refresh", "< Prev", "Next >", "Back"};
        for (String a : actions) {
            UI.button(ctx, px, by, pw, 15, a, mx >= px && mx < px + pw && my >= by && my < by + 15, a.equals("Open"));
            by += 19;
        }
        if (selected != null) {
            by += 6;
            UI.small(ctx, "SELECTED", px, by, t.textDim());
            UI.small(ctx, UI.trim(selected.getFileName().toString(), pw / 0.75f), px, by + 9, t.text());
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(Instant.ofEpochMilli(selected.toFile().lastModified()).atZone(ZoneId.systemDefault()));
            UI.small(ctx, date, px, by + 18, t.textDim());
            rename.bounds(px, by + 30, pw, 14);
            rename.render(ctx, mx, my, delta);
            UI.small(ctx, "Enter to rename", px, by + 47, t.textDim());
        }
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (rename.mouseClicked(mx, my, button)) return true;
        float gx = 20, gy = 40, gw = vw - 200, gh = vh - 70;
        float cw = (gw - (COLS - 1) * 8) / COLS, ch = (gh - (ROWS - 1) * 8) / ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = page * COLS * ROWS + i;
            if (idx >= files.size()) break;
            float x = gx + (i % COLS) * (cw + 8), y = gy + (i / COLS) * (ch + 8);
            if (mx >= x && mx < x + cw && my >= y && my < y + ch) {
                selected = files.get(idx);
                rename.setText(selected.getFileName().toString().replaceFirst("\\.png$", ""));
                ModuleManager.playClick();
                return true;
            }
        }
        float px = vw - 170, pw = 150;
        if (mx < px || mx >= px + pw) return false;
        int a = (int) ((my - 40) / 19);
        ModuleManager.playClick();
        switch (a) {
            case 0 -> {
                if (selected != null) Util.getOperatingSystem().open(selected.toFile());
            }
            case 1 -> {
                if (selected != null) {
                    client.keyboard.setClipboard(selected.toAbsolutePath().toString());
                    Notifications.info("Copied", "Path copied to clipboard");
                }
            }
            case 2 -> {
                if (selected != null) {
                    try {
                        releaseAll();
                        Files.deleteIfExists(selected);
                        Notifications.info("Deleted", selected.getFileName().toString());
                    } catch (IOException e) {
                        Notifications.error("Delete failed", e.getMessage());
                    }
                    selected = null;
                    reload();
                }
            }
            case 3 -> Util.getOperatingSystem().open(dir().toFile());
            case 4 -> {
                releaseAll();
                reload();
            }
            case 5 -> {
                if (page > 0) {
                    page--;
                    releaseAll();
                }
            }
            case 6 -> {
                if ((page + 1) * COLS * ROWS < files.size()) {
                    page++;
                    releaseAll();
                }
            }
            case 7 -> close();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean onKey(int key, int scancode, int mods) {
        if (rename.capturesKeyboard()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && selected != null) {
                String n = rename.getText().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
                if (!n.isEmpty()) {
                    try {
                        releaseAll();
                        Path target = selected.resolveSibling(n + ".png");
                        if (!Files.exists(target)) {
                            Files.move(selected, target);
                            selected = target;
                            Notifications.success("Renamed", target.getFileName().toString());
                        }
                    } catch (IOException e) {
                        Notifications.error("Rename failed", e.getMessage());
                    }
                    reload();
                }
                rename.setFocused(false);
                return true;
            }
            return rename.keyPressed(key, scancode, mods);
        }
        return false;
    }

    @Override
    protected boolean onChar(String chars) { return rename.capturesKeyboard() && rename.charTyped(chars); }
}
