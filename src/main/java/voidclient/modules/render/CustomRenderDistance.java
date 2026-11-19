package voidclient.modules.render;

import net.minecraft.world.World;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.keybinds.Keybind;

/** Per-dimension render distance presets plus quick +/- keys. */
public class CustomRenderDistance extends Module {
    public final NumberSetting overworld = add(new NumberSetting("Overworld", "Render distance in the Overworld.", 12, 2, 32, 1));
    public final NumberSetting nether = add(new NumberSetting("Nether", "Render distance in the Nether.", 8, 2, 32, 1));
    public final NumberSetting end = add(new NumberSetting("End", "Render distance in the End.", 10, 2, 32, 1));
    public final BooleanSetting syncSimulation = add(new BooleanSetting("Sync Simulation", "Also set simulation distance (singleplayer).", false));
    public final KeybindSetting increase = add(new KeybindSetting("Increase Key", "Add one chunk to the current dimension.", Keybind.NONE));
    public final KeybindSetting decrease = add(new KeybindSetting("Decrease Key", "Remove one chunk from the current dimension.", Keybind.NONE));
    private Object lastDimension;

    public CustomRenderDistance() {
        super("Custom Render Distance", "Different render distances per dimension.", Category.RENDER);
        on(Events.TickPost.class, e -> {
            if (mc.world == null) return;
            Object dim = mc.world.getRegistryKey();
            if (dim != lastDimension) {
                lastDimension = dim;
                apply();
            }
        });
        on(Events.Key.class, e -> {
            if (e.action != 1 || mc.currentScreen != null || mc.world == null) return;
            NumberSetting s = current();
            if (increase.get().matchesKey(e.key)) s.set(s.get() + 1);
            else if (decrease.get().matchesKey(e.key)) s.set(s.get() - 1);
            else return;
            apply();
            Notifications.info("Render Distance", s.getInt() + " chunks");
        });
    }

    private NumberSetting current() {
        if (mc.world == null) return overworld;
        if (mc.world.getRegistryKey() == World.NETHER) return nether;
        if (mc.world.getRegistryKey() == World.END) return end;
        return overworld;
    }

    private void apply() {
        int d = current().getInt();
        mc.options.getViewDistance().setValue(d);
        if (syncSimulation.get()) mc.options.getSimulationDistance().setValue(Math.max(5, Math.min(32, d)));
    }

    @Override
    protected void onEnable() { if (mc.world != null) apply(); }

    @Override
    public String getInfo() { return current().getInt() + "ch"; }
}
