package voidclient.modules.misc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.animations.Animation;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.HashMap;
import java.util.Map;

/** WASD, space and mouse buttons with press animation and CPS. */
public class Keystrokes extends HudModule {
    public final BooleanSetting mouse = add(new BooleanSetting("Mouse Buttons", "Show LMB/RMB.", true));
    public final BooleanSetting space = add(new BooleanSetting("Space Bar", "Show the jump key.", true));
    public final BooleanSetting cps = add(new BooleanSetting("CPS", "Clicks per second under mouse buttons.", true));
    public final NumberSetting keySize = add(new NumberSetting("Key Size", "Key size.", 22, 14, 40, 1));
    public final NumberSetting gap = add(new NumberSetting("Gap", "Gap between keys.", 2, 0, 6, 0.5));
    public final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", "Fill when pressed.", 0xE6B026FF).themed());
    public final ColorSetting keyColor = add(new ColorSetting("Key Color", "Fill when released.", 0x99080812));
    public final NumberSetting animation = add(new NumberSetting("Animation", "Press fade length.", 120, 0, 400, 10).unit("ms"));

    private final Map<String, Animation> anims = new HashMap<>();

    public Keystrokes() {
        super("Keystrokes", "Live keystroke display with CPS.", Category.MISC, 0.005, 0.455);
        alsoListIn(Category.COMBAT);
        background.defaultTo(false);
    }

    private void key(DrawContext ctx, String id, String label, float x, float y, float w, float h, boolean pressed) {
        Animation a = anims.computeIfAbsent(id, k -> new Animation(0, animation.get()));
        a.target(pressed ? 1 : 0);
        float p = animation.get() <= 0 ? (pressed ? 1 : 0) : a.get();
        int fill = ColorUtil.lerp(keyColor.getColor(), pressedColor.getColor(), p);
        if (p > 0.05f) Render2D.glow(ctx, x, y, w, h, 4, 0.5f * p, pressedColor.getColor());
        Render2D.roundRect(ctx, x, y, w, h, 4, fill);
        if (border.get()) Render2D.roundRectOutline(ctx, x, y, w, h, 4, 0.7f, ColorUtil.withAlpha(borderColor.getColor(), 120));
        float tw = textWidth(label);
        text(ctx, label, x + (w - tw) / 2f, y + (h - 8) / 2f, ColorUtil.lerp(textColor.getColor(), 0xFFFFFFFF, p));
    }

    private boolean down(KeyBinding k) { return k.isPressed(); }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float s = keySize.getFloat(), g = gap.getFloat();
        float w = 3 * s + 2 * g;
        var o = mc.options;
        key(ctx, "w", "W", s + g, 0, s, s, down(o.forwardKey));
        key(ctx, "a", "A", 0, s + g, s, s, down(o.leftKey));
        key(ctx, "s", "S", s + g, s + g, s, s, down(o.backKey));
        key(ctx, "d", "D", 2 * (s + g), s + g, s, s, down(o.rightKey));
        float y = 2 * (s + g);
        if (mouse.get()) {
            float mw = (w - g) / 2f, mh = cps.get() ? s * 0.9f : s * 0.7f;
            CpsCounter c = CpsCounter.INSTANCE;
            String l = cps.get() && c != null ? "LMB " + c.left() : "LMB";
            String r = cps.get() && c != null ? "RMB " + c.right() : "RMB";
            key(ctx, "lmb", l, 0, y, mw, mh, down(o.attackKey));
            key(ctx, "rmb", r, mw + g, y, mw, mh, down(o.useKey));
            y += mh + g;
        }
        if (space.get()) {
            float sh = s * 0.5f;
            key(ctx, "space", "▬▬▬", 0, y, w, sh, down(o.jumpKey));
            y += sh;
        }
        size(w, y);
    }
}
