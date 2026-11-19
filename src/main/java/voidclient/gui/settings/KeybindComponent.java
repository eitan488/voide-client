package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.core.Module;
import voidclient.core.ModuleManager;
import voidclient.core.setting.KeybindSetting;
import voidclient.gui.components.UI;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.keybinds.Keybind;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.List;

/** Click to listen; next key or mouse button binds it. Esc/Backspace clears. Shows conflicts. */
public class KeybindComponent extends SettingComponent<KeybindSetting> {
    private final Module owner;
    private boolean listening;
    private List<String> conflicts = List.of();
    private long conflictCheck;

    public KeybindComponent(KeybindSetting s, Module owner) {
        super(s);
        this.owner = owner;
    }

    @Override
    public float height() { return conflicts.isEmpty() ? 18 : 28; }

    @Override
    public boolean capturesKeyboard() { return listening; }

    public boolean isListening() { return listening; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        Theme t = UI.t();
        if (System.currentTimeMillis() - conflictCheck > 500) {
            conflictCheck = System.currentTimeMillis();
            conflicts = owner == null ? List.of() : VoidClient.get().keybinds().conflicts(owner, setting.get());
        }
        label(ctx, y + 5);
        Keybind kb = setting.get();
        String txt = listening ? "Press a key..." : kb.displayName();
        float bw = Math.max(40, UI.width(txt) + 12);
        float holdW = 30;
        float bx = x + w - bw - holdW - 4;
        boolean hover = mx >= bx && mx < bx + bw && my >= y && my < y + 18;
        Render2D.roundRect(ctx, bx, y + 1.5f, bw, 15, 4, listening ? ColorUtil.withAlpha(ThemeManager.accent(0), 80) : hover ? t.surfaceHover() : ColorUtil.withAlpha(t.surfaceHover(), 160));
        Render2D.roundRectOutline(ctx, bx, y + 1.5f, bw, 15, 4, 0.8f, listening ? ThemeManager.accent(0) : conflicts.isEmpty() ? t.stroke() : t.warning());
        UI.text(ctx, txt, bx + (bw - UI.width(txt)) / 2f, y + 5, kb.isBound() || listening ? t.text() : t.textDim());
        float hx = x + w - holdW;
        UI.chip(ctx, hx, y + 3, holdW, 12, "Hold", kb.hold(), mx >= hx && mx < x + w && my >= y && my < y + 18);
        if (!conflicts.isEmpty()) {
            UI.small(ctx, UI.trim("Conflicts: " + String.join(", ", conflicts), (w) / 0.75f), x, y + 20, t.warning());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening) {
            setting.set(new Keybind(button, true, setting.get().hold()));
            listening = false;
            ModuleManager.playClick();
            return true;
        }
        if (!hovered(mx, my) || my > y + 18) return false;
        float holdW = 30;
        if (mx >= x + w - holdW) {
            setting.set(setting.get().withHold(!setting.get().hold()));
            ModuleManager.playClick();
            return true;
        }
        if (button == 1) {
            setting.set(Keybind.NONE);
            return true;
        }
        listening = true;
        ModuleManager.playClick();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (!listening) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) setting.set(Keybind.NONE);
        else setting.set(new Keybind(key, false, setting.get().hold()));
        listening = false;
        conflictCheck = 0;
        return true;
    }
}
