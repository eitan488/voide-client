package voidclient.modules.player;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.utilities.Sounds;

import java.util.List;
import java.util.Locale;

/** Hunger, saturation, held food value and an eat reminder. */
public class FoodHud extends TextHudModule {
    public final BooleanSetting saturation = add(new BooleanSetting("Saturation", "Show saturation (hidden in vanilla).", true));
    public final BooleanSetting heldFood = add(new BooleanSetting("Held Food Value", "Nutrition and saturation of the food in your hand.", true));
    public final NumberSetting remindBelow = add(new NumberSetting("Remind Below", "Warn when hunger drops to this (0 = off).", 6, 0, 20, 1));
    public final SoundSetting reminderSound = add(new SoundSetting("Reminder Sound", "Played once when the reminder triggers.", Sounds.ClientSound.WARNING, true, 0.6));
    private boolean warned;

    public FoodHud() {
        super("Food HUD", "Hunger, saturation and food values at a glance.", Category.PLAYER, 0.80, 0.70);
        on(Events.TickPost.class, e -> {
            if (mc.player == null || remindBelow.getInt() == 0) return;
            int f = mc.player.getHungerManager().getFoodLevel();
            if (f <= remindBelow.getInt() && !warned) {
                warned = true;
                reminderSound.play();
                voidclient.gui.Notifications.warning("Food", "Hunger is " + f + "/20 - eat something");
            } else if (f > remindBelow.getInt()) warned = false;
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("Food", "18/20"));
            return;
        }
        var hm = mc.player.getHungerManager();
        int f = hm.getFoodLevel();
        out.add(new Line("Food", f + "/20", f <= remindBelow.getInt() ? 0xFFFF5566 : 0));
        if (saturation.get()) out.add(new Line("Saturation", String.format(Locale.ROOT, "%.1f", hm.getSaturationLevel())));
        if (heldFood.get()) {
            FoodComponent food = mc.player.getMainHandStack().get(DataComponentTypes.FOOD);
            if (food == null) food = mc.player.getOffHandStack().get(DataComponentTypes.FOOD);
            if (food != null) out.add(new Line("Held", String.format(Locale.ROOT, "+%d / +%.1f", food.nutrition(), food.saturation())));
        }
    }
}
