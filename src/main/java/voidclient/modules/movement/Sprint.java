package voidclient.modules.movement;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Keeps you sprinting while moving forward (same as holding the sprint key). */
public class Sprint extends HudModule {
    public final BooleanSetting showStatus = add(new BooleanSetting("Show Status", "Display [Sprinting] on the HUD.", true));
    public final BooleanSetting whileUsing = add(new BooleanSetting("Keep While Using Items", "Hold sprint even while using an item.", false));
    public final NumberSetting minFood = add(new NumberSetting("Min Food", "Do not sprint at or below this hunger (vanilla 6).", 6, 0, 20, 1));

    public Sprint() {
        super("Sprint", "Toggle sprint: sprint automatically while moving forward.", Category.MOVEMENT, 0.005, 0.965);
        on(Events.TickPre.class, e -> {
            if (!inWorld()) return;
            var p = mc.player;
            boolean can = p.input.hasForwardMovement() && !p.isSneaking() && !p.horizontalCollision
                    && (p.getAbilities().allowFlying || p.getHungerManager().getFoodLevel() > minFood.getInt())
                    && (whileUsing.get() || !p.isUsingItem());
            if (can) mc.options.sprintKey.setPressed(true);
        });
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.sprintKey.setPressed(false);
    }

    @Override
    public boolean shouldRender() { return showStatus.get(); }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        boolean sprinting = mc.player != null && mc.player.isSprinting();
        String s = sprinting ? "[Sprinting (Toggled)]" : "[Sprint Toggled]";
        float w = textWidth(s) + 8;
        size(w, 13);
        panel(ctx, 0, 0, w, 13);
        text(ctx, s, 4, 2.5f, sprinting ? accent() : textColor.getColor());
    }
}
