package voidclient.gui.settings;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.StringSetting;
import voidclient.gui.components.TextField;

public class StringComponent extends SettingComponent<StringSetting> {
    private final TextField field;

    public StringComponent(StringSetting s) {
        super(s);
        field = new TextField(s.getDescription(), s.getMaxLength());
        field.setText(s.get());
        field.onChange(s::set);
    }

    @Override
    public float height() { return 32; }

    @Override
    public boolean capturesKeyboard() { return field.capturesKeyboard(); }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        if (!field.isFocused() && !field.getText().equals(setting.get())) field.setText(setting.get());
        label(ctx, y + 2);
        field.bounds(x, y + 14, w, 15);
        field.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (resetOnRightClick(mx, my, button, 12)) return true;
        return field.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) { return field.keyPressed(key, scancode, mods); }

    @Override
    public boolean charTyped(String chars) { return field.charTyped(chars); }
}
