package voidclient.modules.world;

import org.lwjgl.glfw.GLFW;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.gui.screens.WorldMapScreen;
import voidclient.keybinds.Keybind;

/** Opens the explored world map (regions saved per world and dimension). */
public class WorldMap extends Module {
    public final BooleanSetting record = add(new BooleanSetting("Record Terrain", "Draw newly loaded chunks into the map.", true));

    public WorldMap() {
        super("World Map", "Explored-terrain world map with waypoints and inspected chunks.", Category.WORLD);
        keybind.defaultTo(Keybind.key(GLFW.GLFW_KEY_M));
        toggleNotification.defaultTo(false);
        MapCache.init();
        record.onChange(MapCache::setEnabled);
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() {
        if (mc.world == null) return;
        mc.setScreen(new WorldMapScreen(mc.currentScreen));
    }
}
