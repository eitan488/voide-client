package voidclient.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Single-line text input with cursor, clipboard and validation. */
public class TextField extends Widget {
    private String text = "";
    private int cursor;
    private boolean focused;
    private final String placeholder;
    private final int maxLength;
    private Consumer<String> onChange = s -> {};
    private Consumer<String> onSubmit = s -> {};
    private Predicate<String> filter = s -> true;
    private long blinkStart = System.currentTimeMillis();

    public TextField(String placeholder, int maxLength) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
    }

    public TextField onChange(Consumer<String> c) { this.onChange = c; return this; }
    public TextField onSubmit(Consumer<String> c) { this.onSubmit = c; return this; }
    public TextField filter(Predicate<String> f) { this.filter = f; return this; }

    public String getText() { return text; }

    public void setText(String s) {
        text = s == null ? "" : s.length() > maxLength ? s.substring(0, maxLength) : s;
        cursor = text.length();
    }

    public boolean isFocused() { return focused; }

    public void setFocused(boolean f) {
        focused = f;
        blinkStart = System.currentTimeMillis();
    }

    @Override
    public boolean capturesKeyboard() { return focused; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = UI.t();
        boolean hover = hovered(mx, my);
        Render2D.roundRect(ctx, x, y, w, h, 4, ColorUtil.withAlpha(t.background(), 200));
        Render2D.roundRectOutline(ctx, x, y, w, h, 4, focused ? 1f : 0.7f, focused ? ThemeManager.accent(0) : hover ? ColorUtil.withAlpha(t.text(), 80) : t.stroke());
        float ty = y + (h - 8) / 2f;
        Render2D.scissor(ctx, x + 3, y, w - 6, h);
        String shown = text;
        float tw = UI.width(shown.substring(0, Math.min(cursor, shown.length())));
        float scroll = Math.max(0, tw - (w - 12));
        if (shown.isEmpty() && !focused) {
            UI.text(ctx, placeholder, x + 5, ty, ColorUtil.withAlpha(t.textDim(), 180));
        } else {
            UI.text(ctx, shown, x + 5 - scroll, ty, t.text());
        }
        if (focused && ((System.currentTimeMillis() - blinkStart) / 500) % 2 == 0) {
            Render2D.rect(ctx, x + 5 - scroll + tw, ty - 1, 0.8f, 10, ThemeManager.accent(0));
        }
        Render2D.endScissor(ctx);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean inside = hovered(mx, my);
        setFocused(inside);
        if (inside && button == 1) {
            setText("");
            onChange.accept(text);
        } else if (inside) {
            float rel = (float) mx - x - 5;
            int c = 0;
            while (c < text.length() && UI.width(text.substring(0, c + 1)) < rel) c++;
            cursor = c;
        }
        return inside;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (!focused) return false;
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0 || (mods & GLFW.GLFW_MOD_SUPER) != 0;
        blinkStart = System.currentTimeMillis();
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    int from = ctrl ? wordStart() : cursor - 1;
                    text = text.substring(0, from) + text.substring(cursor);
                    cursor = from;
                    onChange.accept(text);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < text.length()) {
                    text = text.substring(0, cursor) + text.substring(cursor + 1);
                    onChange.accept(text);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(text.length(), cursor + 1);
            case GLFW.GLFW_KEY_HOME -> cursor = 0;
            case GLFW.GLFW_KEY_END -> cursor = text.length();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onSubmit.accept(text);
                setFocused(false);
            }
            case GLFW.GLFW_KEY_ESCAPE -> setFocused(false);
            case GLFW.GLFW_KEY_A -> {
                if (!ctrl) return true;
                cursor = text.length();
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl) MinecraftClient.getInstance().keyboard.setClipboard(text);
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl) {
                    MinecraftClient.getInstance().keyboard.setClipboard(text);
                    setText("");
                    onChange.accept(text);
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) insert(MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n\\t]", " "));
            }
            default -> {
            }
        }
        return true;
    }

    private int wordStart() {
        int i = cursor - 1;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return Math.max(0, i);
    }

    private void insert(String s) {
        String next = text.substring(0, cursor) + s + text.substring(cursor);
        if (next.length() > maxLength || !filter.test(next)) return;
        text = next;
        cursor += s.length();
        onChange.accept(text);
    }

    @Override
    public boolean charTyped(String chars) {
        if (!focused) return false;
        insert(chars);
        return true;
    }
}
