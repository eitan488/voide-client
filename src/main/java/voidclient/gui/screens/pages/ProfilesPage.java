package voidclient.gui.screens.pages;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import voidclient.VoidClient;
import voidclient.config.ProfileManager;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.TextField;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.nio.file.Files;
import java.util.List;

/** Create, rename, delete, duplicate, import and export JSON profiles. */
public class ProfilesPage extends Page {
    private final TextField name = new TextField("Profile name (letters, digits, space, _ -)", 32);
    private String selected;
    private boolean confirmDelete;

    public ProfilesPage() {
        selected = VoidClient.get().profiles().active();
        name.filter(ProfileManager::isValidName);
    }

    private ProfileManager pm() { return VoidClient.get().profiles(); }

    @Override
    public boolean capturesKeyboard() { return name.capturesKeyboard(); }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        UI.bold(ctx, "Profiles", x + 2, y + 2, 0xFFFFFFFF);
        UI.small(ctx, "Each profile stores every module, keybind, HUD layout and theme. Saved to config/voidclient/profiles as JSON.", x + 2, y + 14, t.textDim());
        float listW = w * 0.45f, ly = y + 28;
        List<String> profiles = pm().list();
        String active = pm().active();
        float ry = ly;
        for (String p : profiles) {
            boolean hover = in(mx, my, x, ry, listW, 18);
            boolean sel = p.equals(selected);
            Render2D.roundRect(ctx, x, ry, listW, 17, 5, sel ? ColorUtil.withAlpha(ThemeManager.accent(0), 60) : hover ? t.surfaceHover() : ThemeManager.panel(t.surface()));
            if (sel) Render2D.roundRectOutline(ctx, x, ry, listW, 17, 5, 0.8f, ThemeManager.accent(0));
            UI.text(ctx, p, x + 8, ry + 4.5f, t.text());
            if (p.equals(active)) {
                float bw = UI.chipWidth("ACTIVE");
                UI.chip(ctx, x + listW - bw - 4, ry + 2.5f, bw, 12, "ACTIVE", true, false);
            }
            ry += 20;
            if (ry > y + h - 20) break;
        }
        float px = x + listW + 10, pw = w - listW - 10;
        Render2D.roundRect(ctx, px, ly, pw, h - (ly - y), 6, ThemeManager.panel(t.surface()));
        float bx = px + 8, bw = pw - 16, by = ly + 8;
        UI.small(ctx, "SELECTED: " + (selected == null ? "-" : selected), bx, by, t.textDim());
        by += 12;
        String[] actions = {"Load", "Duplicate", "Export (file + clipboard)", "Open Profiles Folder"};
        for (String a : actions) {
            UI.button(ctx, bx, by, bw, 15, a, in(mx, my, bx, by, bw, 15), a.equals("Load"));
            by += 18;
        }
        UI.button(ctx, bx, by, bw, 15, confirmDelete ? "Click again to delete" : "Delete", in(mx, my, bx, by, bw, 15), false);
        if (confirmDelete) Render2D.roundRectOutline(ctx, bx, by, bw, 15, 4, 1f, t.danger());
        by += 24;
        UI.small(ctx, "NAME", bx, by, t.textDim());
        by += 9;
        name.bounds(bx, by, bw, 15);
        name.render(ctx, mx, my, delta);
        by += 19;
        UI.button(ctx, bx, by, bw / 2f - 2, 15, "Create", in(mx, my, bx, by, bw / 2f - 2, 15), false);
        UI.button(ctx, bx + bw / 2f + 2, by, bw / 2f - 2, 15, "Rename", in(mx, my, bx + bw / 2f + 2, by, bw / 2f - 2, 15), false);
        by += 24;
        UI.small(ctx, "IMPORT", bx, by, t.textDim());
        by += 9;
        UI.button(ctx, bx, by, bw / 2f - 2, 15, "From Clipboard", in(mx, my, bx, by, bw / 2f - 2, 15), false);
        UI.button(ctx, bx + bw / 2f + 2, by, bw / 2f - 2, 15, "From imports/", in(mx, my, bx + bw / 2f + 2, by, bw / 2f - 2, 15), false);
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float listW = w * 0.45f, ly = y + 28;
        if (name.mouseClicked(mx, my, button)) return true;
        if (mx >= x && mx < x + listW) {
            List<String> profiles = pm().list();
            int idx = (int) ((my - ly) / 20);
            if (idx >= 0 && idx < profiles.size() && my >= ly) {
                selected = profiles.get(idx);
                confirmDelete = false;
                name.setText(selected);
                ModuleManager.playClick();
                return true;
            }
            return false;
        }
        float px = x + listW + 10, pw = w - listW - 10, bx = px + 8, bw = pw - 16, by = ly + 20;
        if (selected != null) {
            if (in(mx, my, bx, by, bw, 15)) {
                pm().switchTo(selected);
                return click();
            }
            if (in(mx, my, bx, by + 18, bw, 15)) {
                String target = uniqueName(selected + " Copy");
                if (pm().duplicate(selected, target)) Notifications.success("Profile duplicated", target);
                return click();
            }
            if (in(mx, my, bx, by + 36, bw, 15)) {
                var out = pm().export(selected);
                if (out != null) Notifications.success("Exported", out.getFileName().toString() + " (also copied)");
                else Notifications.error("Export failed", selected);
                return click();
            }
            if (in(mx, my, bx, by + 54, bw, 15)) {
                try {
                    Files.createDirectories(pm().profilesDir());
                    Util.getOperatingSystem().open(pm().profilesDir().toFile());
                } catch (Exception e) {
                    Notifications.error("Open folder", e.getMessage());
                }
                return click();
            }
            if (in(mx, my, bx, by + 72, bw, 15)) {
                if (!confirmDelete) {
                    confirmDelete = true;
                } else {
                    if (pm().delete(selected)) Notifications.info("Profile deleted", selected);
                    else Notifications.error("Cannot delete", "The last profile can't be deleted");
                    selected = pm().active();
                    confirmDelete = false;
                }
                return click();
            }
        }
        float cy = by + 72 + 24 + 9 + 19;
        String n = name.getText().trim();
        if (in(mx, my, bx, cy, bw / 2f - 2, 15)) {
            if (pm().create(n, true)) {
                Notifications.success("Profile created", n);
                selected = n;
            } else Notifications.error("Invalid name", "Use 1-32 letters/digits and a new name");
            return click();
        }
        if (in(mx, my, bx + bw / 2f + 2, cy, bw / 2f - 2, 15) && selected != null) {
            if (pm().rename(selected, n)) {
                Notifications.success("Renamed", selected + " -> " + n);
                selected = n;
            } else Notifications.error("Rename failed", "Invalid or existing name");
            return click();
        }
        float iy = cy + 24 + 9;
        if (in(mx, my, bx, iy, bw / 2f - 2, 15)) {
            String imported = pm().importFromClipboard();
            if (imported != null) Notifications.success("Imported", imported);
            else Notifications.error("Import failed", "Clipboard has no Void profile JSON");
            return click();
        }
        if (in(mx, my, bx + bw / 2f + 2, iy, bw / 2f - 2, 15)) {
            int n2 = pm().importFolder();
            Notifications.info("Import", n2 + " profile(s) imported from " + pm().importsDir().getFileName());
            return click();
        }
        return false;
    }

    private boolean click() {
        ModuleManager.playClick();
        return true;
    }

    private String uniqueName(String base) {
        String b = base.length() > 28 ? base.substring(0, 28) : base;
        String n = b;
        int i = 2;
        while (pm().exists(n)) n = b + " " + i++;
        return n;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) { return name.keyPressed(key, scancode, mods); }

    @Override
    public boolean charTyped(String chars) { return name.charTyped(chars); }
}
