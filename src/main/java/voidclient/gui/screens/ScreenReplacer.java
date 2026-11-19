package voidclient.gui.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import voidclient.VoidClient;

/** Swaps vanilla screens for Void screens when enabled (title screen, server list). */
public final class ScreenReplacer {
    private static boolean bypassOnce;

    private ScreenReplacer() {}

    /** Next setScreen call is not replaced (used by "Vanilla Menu" / "Vanilla List" buttons). */
    public static void bypassNext() { bypassOnce = true; }

    public static Screen replace(Screen screen) {
        if (bypassOnce) {
            bypassOnce = false;
            return screen;
        }
        VoidClient v = VoidClient.get();
        if (v == null || screen == null) return screen;
        if (screen.getClass() == TitleScreen.class && v.clientSettings().customMainMenu.get()) return new MainMenuScreen();
        if (screen.getClass() == MultiplayerScreen.class && v.clientSettings().customServerList.get()) {
            return new ServerBrowserScreen(((MultiplayerScreenAccess) screen).voidclient$parent());
        }
        return screen;
    }

    /** Implemented by MultiplayerScreenMixin to expose the parent screen. */
    public interface MultiplayerScreenAccess {
        Screen voidclient$parent();
    }
}
