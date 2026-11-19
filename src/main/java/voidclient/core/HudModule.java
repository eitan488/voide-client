package voidclient.core;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.FontSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.PositionSetting;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/**
 * A module that draws a movable HUD element. Subclasses draw in local coordinates starting
 * at (0,0) and report their size via {@link #size}; the base class handles position,
 * scale, layering and the shared panel style.
 */
public abstract class HudModule extends Module {
    public final PositionSetting position;
    public final NumberSetting scale;
    public final NumberSetting layer;
    public final BooleanSetting background;
    public final ColorSetting backgroundColor;
    public final BooleanSetting border;
    public final ColorSetting borderColor;
    public final NumberSetting cornerRadius;
    public final FontSetting font;
    public final ColorSetting textColor;

    private float width = 40, height = 12;

    protected HudModule(String name, String description, Category category, double defaultX, double defaultY) {
        super(name, description, category);
        position = add(new PositionSetting("Position", "Screen position (fraction of the screen).", defaultX, defaultY));
        scale = add(new NumberSetting("Scale", "Element scale.", 1.0, 0.5, 3.0, 0.05).unit("x"));
        layer = add(new NumberSetting("Layer", "Draw order; higher layers draw on top.", 0, 0, 20, 1));
        background = add(new BooleanSetting("Background", "Draw a glass panel behind the element.", true));
        backgroundColor = add(new ColorSetting("Background Color", "Panel fill colour.", 0x99080812).visibleWhen(background::get));
        border = add(new BooleanSetting("Border", "Neon border around the panel.", true).visibleWhen(background::get));
        borderColor = add(new ColorSetting("Border Color", "Border colour.", 0xCCB026FF).themed().visibleWhen(() -> background.get() && border.get()));
        cornerRadius = add(new NumberSetting("Corner Radius", "Panel corner radius.", 4, 0, 10, 0.5).visibleWhen(background::get));
        font = add(new FontSetting("Font", "Text font for this element.", FontRenderer.Family.INTER, true));
        textColor = add(new ColorSetting("Text Color", "Primary text colour.", 0xFFF2F2FF));
    }

    /** Draw in local space. Call {@link #size} with the content size. */
    protected abstract void renderHud(DrawContext ctx, float delta, boolean editor);

    protected final void size(float w, float h) {
        this.width = Math.max(4, w);
        this.height = Math.max(4, h);
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    /** Effective scale: the element's own scale times the resolution factor. */
    public float getScale() { return scale.getFloat() * resolutionFactor(); }

    /**
     * Positions are screen fractions, so element sizes follow the screen height too: 270 scaled
     * pixels (1080p at GUI scale 4) is the reference the default layouts were designed for.
     */
    public static float resolutionFactor() {
        var vc = voidclient.VoidClient.get();
        if (vc == null || vc.clientSettings() == null || !vc.clientSettings().hudAutoScale.get()) return 1f;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return 1f;
        int h = mc.getWindow().getScaledHeight();
        return h <= 0 ? 1f : Math.max(0.5f, Math.min(3f, h / 270f));
    }

    public float screenX(int sw) { return (float) Math.min(position.pixelX(sw), Math.max(0, sw - width * getScale())); }
    public float screenY(int sh) { return (float) Math.min(position.pixelY(sh), Math.max(0, sh - height * getScale())); }

    /** Whether there is something to draw outside the editor (e.g. no target = nothing). */
    public boolean shouldRender() { return true; }

    public final void render(DrawContext ctx, int sw, int sh, float delta, boolean editor) {
        if (!editor && !shouldRender()) return;
        float s = getScale();
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(screenX(sw), screenY(sh));
        m.scale(s, s);
        renderHud(ctx, delta, editor);
        m.popMatrix();
    }

    // ---- shared drawing helpers ----------------------------------------------------------

    protected final void panel(DrawContext ctx, float x, float y, float w, float h) {
        if (!background.get()) return;
        float r = cornerRadius.getFloat();
        Render2D.shadow(ctx, x, y, w, h, r, 6, 0x50000000);
        Render2D.roundRect(ctx, x, y, w, h, r, backgroundColor.getColor());
        if (border.get()) Render2D.roundRectOutline(ctx, x, y, w, h, r, 1f, borderColor.getColor());
    }

    protected final float text(DrawContext ctx, String s, float x, float y, int color) {
        return FontRenderer.draw(ctx, s, x, y, color, font.family(), font.shadow());
    }

    protected final float textWidth(String s) { return FontRenderer.width(s, font.family()); }

    protected final float fontHeight() { return FontRenderer.height(font.family()); }

    protected final int accent() { return ThemeManager.accent(0); }

    protected final int dim(int color) { return ColorUtil.multiplyAlpha(color, 0.65f); }
}
