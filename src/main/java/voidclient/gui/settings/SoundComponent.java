package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.SoundSetting;
import voidclient.gui.components.UI;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.Sounds;

import java.util.Locale;

/** Sound picker with its own enable, volume and pitch plus a preview button. */
public class SoundComponent extends SettingComponent<SoundSetting> {
    private int dragging; // 1 volume, 2 pitch

    public SoundComponent(SoundSetting s) { super(s); }

    @Override
    public float height() { return 54; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        if (dragging != 0) apply(mx);
        var v = setting.get();
        label(ctx, y + 2);
        UI.toggle(ctx, x + w - 22, y + 1, 20, 10, v.enabled() ? 1 : 0, false);
        float ry = y + 14;
        String name = v.sound().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        UI.button(ctx, x, ry, 12, 12, "<", mx >= x && mx < x + 12 && my >= ry && my < ry + 12, false);
        UI.text(ctx, UI.trim(name, w - 70), x + 16, ry + 2, UI.t().text());
        UI.button(ctx, x + w - 54, ry, 12, 12, ">", mx >= x + w - 54 && mx < x + w - 42 && my >= ry && my < ry + 12, false);
        UI.button(ctx, x + w - 38, ry, 38, 12, "Play", mx >= x + w - 38 && mx < x + w && my >= ry && my < ry + 12, true);
        bar(ctx, y + 32, "Vol", v.volume(), 0, 1, String.format(Locale.ROOT, "%d%%", Math.round(v.volume() * 100)));
        bar(ctx, y + 45, "Pitch", v.pitch(), 0.5, 2, String.format(Locale.ROOT, "%.2fx", v.pitch()));
    }

    private void bar(DrawContext ctx, float yy, String label, double value, double min, double max, String txt) {
        UI.small(ctx, label, x, yy - 1, UI.t().textDim());
        UI.small(ctx, txt, x + w - UI.smallWidth(txt), yy - 1, ThemeManager.accent(0));
        float sx = x + 26, sw = w - 56;
        float f = (float) ((value - min) / (max - min));
        Render2D.roundRect(ctx, sx, yy + 1, sw, 3, 1.5f, ColorUtil.withAlpha(UI.t().textDim(), 60));
        Render2D.roundRectH(ctx, sx, yy + 1, sw * f, 3, 1.5f, ThemeManager.accent(0), ThemeManager.accent(0.33f));
        Render2D.circle(ctx, sx + sw * f, yy + 2.5f, 3f, 0xFFFFFFFF);
    }

    private void apply(double mx) {
        float sx = x + 26, sw = w - 56;
        double f = Math.max(0, Math.min(1, (mx - sx) / sw));
        if (dragging == 1) setting.setVolume(f);
        else setting.setPitch(0.5 + f * 1.5);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (resetOnRightClick(mx, my, button, 12)) return true;
        var v = setting.get();
        if (my < y + 12 && mx >= x + w - 24) {
            setting.setEnabled(!v.enabled());
            return true;
        }
        float ry = y + 14;
        if (my >= ry && my < ry + 12) {
            Sounds.ClientSound[] all = Sounds.ClientSound.values();
            if (mx < x + 12) setting.setSound(all[Math.floorMod(v.sound().ordinal() - 1, all.length)]);
            else if (mx >= x + w - 54 && mx < x + w - 42) setting.setSound(all[(v.sound().ordinal() + 1) % all.length]);
            else if (mx >= x + w - 38) Sounds.play(v.sound(), (float) v.volume(), (float) v.pitch());
            return true;
        }
        if (my >= y + 28 && my < y + 38) dragging = 1;
        else if (my >= y + 41) dragging = 2;
        if (dragging != 0) apply(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging != 0;
        dragging = 0;
        return was;
    }
}
