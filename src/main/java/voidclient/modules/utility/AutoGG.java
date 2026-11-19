package voidclient.modules.utility;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.StringSetting;
import voidclient.events.Events;

import java.util.Locale;

/** Sends a message (default "gg") when a game-end message appears in chat. */
public class AutoGG extends Module {
    public final StringSetting message = add(new StringSetting("Message", "What to send.", "gg", 100));
    public final StringSetting triggers = add(new StringSetting("Triggers", "Comma-separated phrases that mark the end of a game.",
            "winner:,won the game,1st killer,victory!,game over,you won,you lost,match over", 500));
    public final NumberSetting delay = add(new NumberSetting("Delay", "Wait before sending.", 1200, 0, 5000, 100).unit("ms"));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum time between messages.", 15, 3, 120, 1).unit("s"));
    public final BooleanSetting serversOnly = add(new BooleanSetting("Servers Only", "Do nothing in singleplayer.", true));

    private long lastSent, scheduledAt = -1;

    public AutoGG() {
        super("Auto GG", "Says gg automatically when a game ends.", Category.UTILITY);
        on(Events.ChatReceive.class, e -> {
            if (serversOnly.get() && mc.isInSingleplayer()) return;
            String text = e.message.getString().toLowerCase(Locale.ROOT);
            for (String t : triggers.get().split(",")) {
                if (!t.isBlank() && text.contains(t.trim().toLowerCase(Locale.ROOT)) && System.currentTimeMillis() - lastSent > cooldown.get() * 1000) {
                    scheduledAt = System.currentTimeMillis() + delay.getInt();
                    lastSent = System.currentTimeMillis();
                    break;
                }
            }
        });
        on(Events.TickPost.class, e -> {
            if (scheduledAt > 0 && System.currentTimeMillis() >= scheduledAt) {
                scheduledAt = -1;
                if (mc.getNetworkHandler() != null && !message.get().isBlank()) mc.getNetworkHandler().sendChatMessage(message.get());
            }
        });
    }
}
