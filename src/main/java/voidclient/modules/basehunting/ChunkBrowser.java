package voidclient.modules.basehunting;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.gui.screens.ChunkBrowserScreen;

/** Opens the Chunk Browser (history, search, compare, labels, notes, export). */
public class ChunkBrowser extends Module {
    public ChunkBrowser() {
        super("Chunk Browser", "Search, compare, label and export inspected chunks.", Category.BASE_HUNTING);
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() {
        if (mc.world != null) mc.setScreen(new ChunkBrowserScreen(mc.currentScreen));
    }
}
