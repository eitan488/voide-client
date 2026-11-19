package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.ModuleManager;
import voidclient.core.setting.BooleanSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.animations.Easing;
import voidclient.gui.components.UI;

public class BooleanComponent extends SettingComponent<BooleanSetting> {
    private final Animation knob;

    public BooleanComponent(BooleanSetting s) {
        super(s);
        knob = new Animation(s.get() ? 1 : 0, 180, Easing.BACK_OUT);
    }

    @Override
    public float height() { return 18; }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        knob.target(setting.get() ? 1 : 0);
        label(ctx, y + 5);
        UI.toggle(ctx, x + w - 24, y + 3.5f, 22, 11, (float) Math.max(0, Math.min(1, knob.update())), hovered(mx, my));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (button == 1) {
            setting.reset();
            return true;
        }
        setting.toggle();
        ModuleManager.playClick();
        return true;
    }
}
