package voidclient.gui.screens;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.gui.Notifications;
import voidclient.render.Render2D;

/**
 * Base for every Void screen. Renders in a virtual coordinate space whose scale keeps the UI
 * the same physical size across 1080p, 1440p and 4K and across Minecraft GUI scales.
 */
public abstract class VoidScreen extends Screen {
    protected final Screen parent;
    protected float scale = 1f;
    protected float vw, vh;
    private final float minW, minH;
    protected double lastMx, lastMy;

    protected VoidScreen(Text title, Screen parent, float minW, float minH) {
        super(title);
        this.parent = parent;
        this.minW = minW;
        this.minH = minH;
    }

    protected float computeScale() {
        var settings = VoidClient.get().clientSettings();
        var window = client.getWindow();
        double f = Math.max(1, window.getScaleFactor());
        float s = settings.autoScale.get() ? (float) ((window.getFramebufferHeight() / 540.0) / f) : 1f;
        s *= settings.guiScale.getFloat();
        float fit = Math.min(width / minW, height / minH);
        return Math.max(0.3f, Math.min(s, fit));
    }

    @Override
    protected final void init() {
        scale = computeScale();
        vw = width / scale;
        vh = height / scale;
        layout();
    }

    protected abstract void layout();

    protected abstract void renderVoid(DrawContext ctx, double mx, double my, float delta);

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        lastMx = mouseX / scale;
        lastMy = mouseY / scale;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(scale, scale);
        renderVoid(ctx, lastMx, lastMy, delta);
        m.popMatrix();
        Notifications.render(ctx, width, height);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world != null) {
            if (VoidClient.get().clientSettings().blurBackground.get()) applyBlur(ctx);
            Render2D.gradientV(ctx, 0, 0, width, height, 0x66000000, 0x99000008);
        } else {
            MenuBackground.render(ctx, width, height, delta);
        }
    }

    // ---- input bridge ------------------------------------------------------------------------

    protected boolean onClick(double mx, double my, int button) { return false; }
    protected boolean onRelease(double mx, double my, int button) { return false; }
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) { return false; }
    protected boolean onScroll(double mx, double my, double amount) { return false; }
    protected boolean onKey(int key, int scancode, int mods) { return false; }
    protected boolean onChar(String chars) { return false; }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return onClick(click.x() / scale, click.y() / scale, click.button()) || super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return onRelease(click.x() / scale, click.y() / scale, click.button()) || super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        return onDrag(click.x() / scale, click.y() / scale, click.button(), dx / scale, dy / scale) || super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        return onScroll(mx / scale, my / scale, v) || super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (onKey(input.key(), input.scancode(), input.modifiers())) return true;
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (input.isValidChar() && onChar(input.asString())) return true;
        return super.charTyped(input);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
