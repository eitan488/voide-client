package voidclient.gui.components;

import net.minecraft.client.gui.DrawContext;

/** Minimal retained-mode widget in Void GUI space (already divided by the GUI scale). */
public abstract class Widget {
    public float x, y, w, h;
    public boolean visible = true;

    public Widget bounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public boolean hovered(double mx, double my) {
        return visible && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public abstract void render(DrawContext ctx, double mx, double my, float delta);

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public boolean mouseReleased(double mx, double my, int button) { return false; }
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return false; }
    public boolean mouseScrolled(double mx, double my, double amount) { return false; }
    public boolean keyPressed(int key, int scancode, int mods) { return false; }
    public boolean charTyped(String chars) { return false; }

    /** True while the widget captures keyboard input (text field, keybind listener). */
    public boolean capturesKeyboard() { return false; }
}
