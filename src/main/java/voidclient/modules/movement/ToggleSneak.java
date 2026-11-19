package voidclient.modules.movement;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.events.Events;

/** Press sneak once to keep sneaking, press again to stop. */
public class ToggleSneak extends HudModule {
    public final BooleanSetting showStatus = add(new BooleanSetting("Show Status", "Display [Sneaking] on the HUD.", true));
    public final BooleanSetting releaseWhenFlying = add(new BooleanSetting("Release While Flying", "Do not hold sneak while flying (so you can descend normally).", true));

    private boolean toggled;

    public ToggleSneak() {
        super("Toggle Sneak", "Tap sneak to toggle sneaking on and off.", Category.MOVEMENT, 0.16, 0.965);
        on(Events.Key.class, e -> {
            if (mc.currentScreen != null || e.action != 1) return;
            if (mc.options.sneakKey.matchesKey(new net.minecraft.client.input.KeyInput(e.key, e.scancode, e.modifiers))) toggled = !toggled;
        });
        on(Events.TickPre.class, e -> {
            if (!inWorld()) return;
            boolean flying = mc.player.getAbilities().flying;
            if (toggled && !(flying && releaseWhenFlying.get())) mc.options.sneakKey.setPressed(true);
        });
    }

    @Override
    protected void onDisable() {
        toggled = false;
        if (mc.options != null) mc.options.sneakKey.setPressed(false);
    }

    @Override
    public boolean shouldRender() { return showStatus.get() && toggled; }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        String s = "[Sneaking (Toggled)]";
        float w = textWidth(s) + 8;
        size(w, 13);
        panel(ctx, 0, 0, w, 13);
        text(ctx, s, 4, 2.5f, accent());
    }
}
