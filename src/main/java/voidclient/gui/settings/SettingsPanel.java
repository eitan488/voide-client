package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.FontSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.PositionSetting;
import voidclient.core.setting.RangeSetting;
import voidclient.core.setting.Setting;
import voidclient.core.setting.SoundSetting;
import voidclient.core.setting.StringSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.components.UI;
import voidclient.gui.components.Widget;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/** Scrollable list of setting components for one module (or the client settings). */
public class SettingsPanel extends Widget {
    private final List<SettingComponent<?>> components = new ArrayList<>();
    private final Animation scroll = new Animation(0, 160);
    private float scrollTarget;
    private float contentHeight;
    private SettingComponent<?> hoveredComponent;

    public SettingsPanel(List<Setting<?>> settings, Module owner) {
        for (Setting<?> s : settings) {
            SettingComponent<?> c = create(s, owner);
            if (c != null) components.add(c);
        }
    }

    public static SettingComponent<?> create(Setting<?> s, Module owner) {
        if (s instanceof BooleanSetting b) return new BooleanComponent(b);
        if (s instanceof NumberSetting n) return new NumberComponent(n);
        if (s instanceof ModeSetting m) return new ModeComponent(m);
        if (s instanceof ColorSetting c) return new ColorComponent(c);
        if (s instanceof KeybindSetting k) return new KeybindComponent(k, owner);
        if (s instanceof StringSetting st) return new StringComponent(st);
        if (s instanceof MultiSelectSetting ms) return new MultiSelectComponent(ms);
        if (s instanceof RangeSetting r) return new RangeComponent(r);
        if (s instanceof PositionSetting p) return new PositionComponent(p);
        if (s instanceof FontSetting f) return new FontComponent(f);
        if (s instanceof SoundSetting so) return new SoundComponent(so);
        return null;
    }

    public SettingComponent<?> hoveredComponent() { return hoveredComponent; }

    @Override
    public boolean capturesKeyboard() {
        for (SettingComponent<?> c : components) if (c.isVisibleSetting() && c.capturesKeyboard()) return true;
        return false;
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        float maxScroll = Math.max(0, contentHeight - h);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.target(scrollTarget);
        float off = scroll.get();
        Render2D.scissor(ctx, x, y, w, h);
        float cy = y - off;
        hoveredComponent = null;
        boolean inside = mx >= x && mx < x + w && my >= y && my < y + h;
        for (SettingComponent<?> c : components) {
            if (!c.isVisibleSetting()) continue;
            c.layout(x + 2, cy, w - 8);
            if (cy + c.h > y - 40 && cy < y + h + 40) {
                c.render(ctx, inside ? mx : -9999, inside ? my : -9999, delta);
                if (inside && c.hovered(mx, my)) hoveredComponent = c;
            }
            cy += c.h + 5;
            if (c != components.get(components.size() - 1)) {
                Render2D.rect(ctx, x + 2, cy - 3, w - 8, 0.5f, ColorUtil.withAlpha(UI.t().stroke(), 60));
            }
        }
        contentHeight = cy + off - y;
        Render2D.endScissor(ctx);
        if (contentHeight > h) {
            float barH = Math.max(14, h * h / contentHeight);
            float barY = y + (h - barH) * (off / Math.max(1, contentHeight - h));
            Render2D.roundRect(ctx, x + w - 3, barY, 2, barH, 1, ColorUtil.withAlpha(ThemeManager.accent(0), 140));
        }
    }

    private boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean in = inside(mx, my);
        boolean handled = false;
        for (SettingComponent<?> c : components) {
            if (!c.isVisibleSetting()) continue;
            if (!in) {
                if (c.capturesKeyboard() && !(c instanceof KeybindComponent)) c.mouseClicked(-9999, -9999, button);
                continue;
            }
            if (!handled && c.mouseClicked(mx, my, button)) handled = true;
            else if (c.capturesKeyboard() && !(c instanceof KeybindComponent)) c.mouseClicked(-9999, -9999, button);
        }
        for (SettingComponent<?> c : components) {
            if (c instanceof KeybindComponent k && k.isListening() && !handled) {
                return k.mouseClicked(mx, my, button);
            }
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean any = false;
        for (SettingComponent<?> c : components) any |= c.mouseReleased(mx, my, button);
        return any;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my)) return false;
        for (SettingComponent<?> c : components) if (c.isVisibleSetting() && c.mouseScrolled(mx, my, amount)) return true;
        scrollTarget -= (float) amount * 24;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        for (SettingComponent<?> c : components) if (c.isVisibleSetting() && c.capturesKeyboard() && c.keyPressed(key, scancode, mods)) return true;
        return false;
    }

    @Override
    public boolean charTyped(String chars) {
        for (SettingComponent<?> c : components) if (c.isVisibleSetting() && c.capturesKeyboard() && c.charTyped(chars)) return true;
        return false;
    }
}
