package voidclient.gui.screens;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.core.HudModule;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.List;
import java.util.Locale;

/**
 * Standalone HUD editor (Minecraft scaled coordinates): drag, resize via corner handle or
 * scroll, snap-to-grid, alignment guides, layer ordering, visibility list and preview mode.
 */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule selected, dragging, resizing;
    private double grabX, grabY, startScale, startW;
    private boolean preview, listOpen = true;
    private float guideX = -1, guideY = -1;
    private float listScroll;
    private static final float LIST_W = 130;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    private List<HudModule> elements() { return VoidClient.get().hud().sortedElements(); }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world == null) MenuBackground.render(ctx, width, height, delta);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme t = ThemeManager.get();
        var settings = VoidClient.get().clientSettings();
        if (!preview) {
            Render2D.rect(ctx, 0, 0, width, height, 0x30000000);
            if (settings.hudSnap.get()) {
                int g = settings.hudGrid.getInt();
                int c = ColorUtil.withAlpha(t.text(), 18);
                for (int x = 0; x < width; x += g) Render2D.rect(ctx, x, 0, 0.5f, height, c);
                for (int y = 0; y < height; y += g) Render2D.rect(ctx, 0, y, width, 0.5f, c);
            }
            Render2D.rect(ctx, width / 2f - 0.25f, 0, 0.5f, height, ColorUtil.withAlpha(ThemeManager.accent(0), 40));
            Render2D.rect(ctx, 0, height / 2f - 0.25f, width, 0.5f, ColorUtil.withAlpha(ThemeManager.accent(0), 40));
        }
        for (HudModule h : elements()) {
            if (!h.isEnabled()) continue;
            h.render(ctx, width, height, delta, true);
            if (preview) continue;
            float x = h.screenX(width), y = h.screenY(height), w = h.getWidth() * h.getScale(), hh = h.getHeight() * h.getScale();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + hh;
            int c = h == selected ? ThemeManager.accent(0) : hover ? ColorUtil.withAlpha(t.text(), 160) : ColorUtil.withAlpha(t.text(), 60);
            Render2D.outline(ctx, x - 1, y - 1, w + 2, hh + 2, h == selected ? 1f : 0.6f, c);
            if (h == selected || hover) {
                Render2D.rect(ctx, x + w - 3, y + hh - 3, 5, 5, ThemeManager.accent(0));
                UI.small(ctx, h.getName(), x, y - 8, c);
            }
        }
        if (!preview) {
            if (guideX >= 0) Render2D.rect(ctx, guideX - 0.5f, 0, 1, height, t.warning());
            if (guideY >= 0) Render2D.rect(ctx, 0, guideY - 0.5f, width, 1, t.warning());
            if (selected != null) renderInfo(ctx);
            if (listOpen) renderList(ctx, mouseX, mouseY);
            String help = "Drag: move   Corner/Scroll: scale   Arrows: nudge   PgUp/PgDn: layer   Del: hide   R: reset   G: grid   P: preview   L: list";
            float hw = UI.smallWidth(help) + 12;
            Render2D.roundRect(ctx, (width - hw) / 2f, height - 14, hw, 11, 4, 0xB0000000);
            UI.small(ctx, help, (width - hw) / 2f + 6, height - 12, t.textDim());
        } else {
            UI.small(ctx, "PREVIEW - press P to edit", 4, 4, ColorUtil.withAlpha(t.textDim(), 160));
        }
        Notifications.render(ctx, width, height);
    }

    private void renderInfo(DrawContext ctx) {
        Theme t = ThemeManager.get();
        HudModule h = selected;
        String info = String.format(Locale.ROOT, "%s  x %d  y %d  scale %.2f  layer %d", h.getName(),
                (int) h.screenX(width), (int) h.screenY(height), h.scale.getFloat(), h.layer.getInt());
        float w = UI.smallWidth(info) + 12;
        Render2D.roundRect(ctx, (width - w) / 2f, 4, w, 12, 4, 0xC0000000);
        Render2D.roundRectOutline(ctx, (width - w) / 2f, 4, w, 12, 4, 0.6f, ThemeManager.accent(0));
        UI.small(ctx, info, (width - w) / 2f + 6, 7, t.text());
    }

    private void renderList(DrawContext ctx, int mx, int my) {
        Theme t = ThemeManager.get();
        float x = width - LIST_W - 4, y = 20, h = height - 40;
        Render2D.roundRect(ctx, x, y, LIST_W, h, 6, ColorUtil.withAlpha(t.background(), 225));
        Render2D.roundRectOutline(ctx, x, y, LIST_W, h, 6, 0.7f, ColorUtil.withAlpha(ThemeManager.accent(0), 140));
        UI.small(ctx, "ELEMENTS (click eye to show/hide)", x + 6, y + 5, t.textDim());
        Render2D.scissor(ctx, x, y + 14, LIST_W, h - 16);
        float ry = y + 15 - listScroll;
        for (HudModule hm : elements()) {
            boolean hover = mx >= x && mx < x + LIST_W && my >= ry && my < ry + 12;
            if (hm == selected) Render2D.roundRect(ctx, x + 3, ry, LIST_W - 6, 11, 3, ColorUtil.withAlpha(ThemeManager.accent(0), 70));
            else if (hover) Render2D.roundRect(ctx, x + 3, ry, LIST_W - 6, 11, 3, t.surfaceHover());
            Render2D.circle(ctx, x + 10, ry + 5.5f, 3, hm.isEnabled() ? t.success() : ColorUtil.withAlpha(t.textDim(), 120));
            UI.small(ctx, UI.trim(hm.getName(), (LIST_W - 40) / 0.75f), x + 17, ry + 2.5f, hm.isEnabled() ? t.text() : t.textDim());
            UI.small(ctx, "L" + hm.layer.getInt(), x + LIST_W - 18, ry + 2.5f, t.textDim());
            ry += 12;
        }
        Render2D.endScissor(ctx);
    }

    private HudModule at(double mx, double my) {
        List<HudModule> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule h = list.get(i);
            if (!h.isEnabled()) continue;
            float x = h.screenX(width), y = h.screenY(height), w = h.getWidth() * h.getScale(), hh = h.getHeight() * h.getScale();
            if (mx >= x - 2 && mx < x + w + 3 && my >= y - 2 && my < y + hh + 3) return h;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (preview) return true;
        if (listOpen && mx >= width - LIST_W - 4 && my >= 20 && my < height - 20) {
            float ry = 35 - listScroll;
            for (HudModule hm : elements()) {
                if (my >= ry && my < ry + 12) {
                    if (mx < width - LIST_W + 12) VoidClient.get().modules().userToggle(hm, false);
                    else selected = hm;
                    ModuleManager.playClick();
                    return true;
                }
                ry += 12;
            }
            return true;
        }
        HudModule h = at(mx, my);
        selected = h;
        if (h == null) return false;
        if (click.button() == 1) {
            VoidClient.get().modules().userToggle(h, false);
            return true;
        }
        float x = h.screenX(width), y = h.screenY(height), w = h.getWidth() * h.getScale(), hh = h.getHeight() * h.getScale();
        if (mx >= x + w - 5 && my >= y + hh - 5) {
            resizing = h;
            startScale = h.scale.getFloat();
            startW = mx - x;
        } else {
            dragging = h;
            grabX = mx - x;
            grabY = my - y;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        var settings = VoidClient.get().clientSettings();
        if (resizing != null) {
            float x = resizing.screenX(width);
            double nw = Math.max(8, mx - x);
            resizing.scale.set(startScale * nw / Math.max(1, startW));
            return true;
        }
        if (dragging == null) return false;
        float w = dragging.getWidth() * dragging.getScale(), h = dragging.getHeight() * dragging.getScale();
        double nx = mx - grabX, ny = my - grabY;
        guideX = guideY = -1;
        if (settings.hudGuides.get()) {
            float thr = 4;
            float[] xs = {0, width / 2f - w / 2f, width - w};
            float[] ys = {0, height / 2f - h / 2f, height - h};
            for (float gx : xs) if (Math.abs(nx - gx) < thr) {
                nx = gx;
                guideX = gx == xs[1] ? width / 2f : gx == 0 ? 0.5f : width - 0.5f;
            }
            for (float gy : ys) if (Math.abs(ny - gy) < thr) {
                ny = gy;
                guideY = gy == ys[1] ? height / 2f : gy == 0 ? 0.5f : height - 0.5f;
            }
            for (HudModule o : elements()) {
                if (o == dragging || !o.isEnabled()) continue;
                float ox = o.screenX(width), oy = o.screenY(height), ow = o.getWidth() * o.getScale(), oh = o.getHeight() * o.getScale();
                if (guideX < 0) {
                    for (float cand : new float[]{ox, ox + ow - w, ox + ow, ox - w}) if (Math.abs(nx - cand) < thr) {
                        nx = cand;
                        guideX = cand == ox || cand == ox + ow - w ? (cand == ox ? ox : ox + ow) : (cand == ox + ow ? ox + ow : ox);
                        break;
                    }
                }
                if (guideY < 0) {
                    for (float cand : new float[]{oy, oy + oh - h, oy + oh, oy - h}) if (Math.abs(ny - cand) < thr) {
                        ny = cand;
                        guideY = cand == oy || cand == oy + oh - h ? (cand == oy ? oy : oy + oh) : (cand == oy + oh ? oy + oh : oy);
                        break;
                    }
                }
            }
        }
        if (settings.hudSnap.get() && guideX < 0) nx = Math.round(nx / settings.hudGrid.get()) * settings.hudGrid.get();
        if (settings.hudSnap.get() && guideY < 0) ny = Math.round(ny / settings.hudGrid.get()) * settings.hudGrid.get();
        nx = Math.max(0, Math.min(width - w, nx));
        ny = Math.max(0, Math.min(height - h, ny));
        dragging.position.setPixels(nx, ny, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        resizing = null;
        guideX = guideY = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (listOpen && mx >= width - LIST_W - 4) {
            listScroll = Math.max(0, listScroll - (float) v * 12);
            return true;
        }
        HudModule target = at(mx, my);
        if (target == null) target = selected;
        if (target == null) return false;
        target.scale.set(target.scale.get() + Math.signum(v) * 0.05);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        boolean shift = input.hasShift();
        switch (key) {
            case GLFW.GLFW_KEY_P -> {
                preview = !preview;
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                VoidClient.get().clientSettings().hudSnap.toggle();
                return true;
            }
            case GLFW.GLFW_KEY_L -> {
                listOpen = !listOpen;
                return true;
            }
            default -> {
            }
        }
        if (selected != null) {
            int step = shift ? 10 : 1;
            float x = selected.screenX(width), y = selected.screenY(height);
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> selected.position.setPixels(x - step, y, width, height);
                case GLFW.GLFW_KEY_RIGHT -> selected.position.setPixels(x + step, y, width, height);
                case GLFW.GLFW_KEY_UP -> selected.position.setPixels(x, y - step, width, height);
                case GLFW.GLFW_KEY_DOWN -> selected.position.setPixels(x, y + step, width, height);
                case GLFW.GLFW_KEY_PAGE_UP -> selected.layer.set(selected.layer.get() + 1);
                case GLFW.GLFW_KEY_PAGE_DOWN -> selected.layer.set(selected.layer.get() - 1);
                case GLFW.GLFW_KEY_DELETE -> {
                    VoidClient.get().modules().userToggle(selected, false);
                    selected = null;
                }
                case GLFW.GLFW_KEY_R -> {
                    selected.position.reset();
                    selected.scale.reset();
                    selected.layer.reset();
                }
                default -> {
                    return super.keyPressed(input);
                }
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        VoidClient.get().config().saveAll();
        client.setScreen(parent);
    }
}
