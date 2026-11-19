package voidclient.modules.utility;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Adds a Reconnect button (with optional countdown) to the disconnect screen. */
public class AutoReconnect extends Module {
    public final BooleanSetting auto = add(new BooleanSetting("Automatic", "Reconnect automatically after the delay.", true));
    public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds before reconnecting.", 5, 1, 60, 1).unit("s"));
    public final NumberSetting maxAttempts = add(new NumberSetting("Max Attempts", "Stop auto-reconnecting after this many tries.", 5, 1, 50, 1));

    private ServerInfo lastServer;
    private int attempts;

    public AutoReconnect() {
        super("Auto Reconnect", "Reconnect button and countdown when you get disconnected.", Category.UTILITY);
        on(Events.WorldJoin.class, e -> {
            if (mc.getCurrentServerEntry() != null) {
                lastServer = mc.getCurrentServerEntry();
                attempts = 0;
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!isEnabled() || !(screen instanceof DisconnectedScreen) || lastServer == null) return;
            long start = System.currentTimeMillis();
            ButtonWidget button = ButtonWidget.builder(Text.literal("Reconnect"), b -> reconnect()).dimensions(w / 2 - 100, h - 30, 200, 20).build();
            Screens.getButtons(screen).add(button);
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                if (!auto.get() || attempts >= maxAttempts.getInt()) {
                    button.setMessage(Text.literal("Reconnect"));
                    return;
                }
                long left = (long) (delay.get() * 1000) - (System.currentTimeMillis() - start);
                button.setMessage(Text.literal("Reconnect in " + Math.max(0, (left + 999) / 1000) + "s"));
                if (left <= 0 && client.currentScreen == screen) reconnect();
            });
        });
    }

    private void reconnect() {
        if (lastServer == null) return;
        attempts++;
        ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(lastServer.address), lastServer, false, null);
    }
}
