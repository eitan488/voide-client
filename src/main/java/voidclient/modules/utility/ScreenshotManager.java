package voidclient.modules.utility;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.screens.ScreenshotBrowserScreen;

/** Opens the screenshot gallery. */
public class ScreenshotManager extends Module {
    public final ModeSetting sort = add(new ModeSetting("Sort", "Gallery order.", "Newest", "Newest", "Oldest", "Name"));

    public ScreenshotManager() {
        super("Screenshot Manager", "Browse, open, rename and delete your screenshots.", Category.UTILITY);
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() { mc.setScreen(new ScreenshotBrowserScreen(mc.currentScreen, sort.get())); }
}
