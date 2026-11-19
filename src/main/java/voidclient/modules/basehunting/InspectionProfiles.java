package voidclient.modules.basehunting;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Seven inspection presets, each with its own component list and minimum count. Always active. */
public class InspectionProfiles extends Module {
    public static final String[] PROFILES = {"Storage Inspection", "Redstone Inspection", "Automated Farm Inspection", "Item Sorter Inspection",
            "Industrial Farm Inspection", "General Structure Inspection", "Custom Inspection"};

    public final ModeSetting active = add(new ModeSetting("Active Profile", "Inspection preset used by Chunk Intelligence.", PROFILES[5], PROFILES));
    private final Map<String, MultiSelectSetting> components = new LinkedHashMap<>();
    private final Map<String, NumberSetting> minimums = new LinkedHashMap<>();

    public InspectionProfiles() {
        super("Inspection Profiles", "Presets for storage, redstone, farm, sorter and structure inspection.", Category.BASE_HUNTING);
        List<String> all = new ArrayList<>();
        for (Component c : Component.values()) all.add(c.label);
        define(PROFILES[0], all, Set.of("Chest", "Trapped Chest", "Barrel", "Shulker Box", "Ender Chest", "Hopper"), 4);
        define(PROFILES[1], all, Set.of("Redstone Dust", "Repeater", "Comparator", "Redstone Torch", "Redstone Block", "Observer", "Piston", "Sticky Piston", "Target Block", "Note Block"), 8);
        define(PROFILES[2], all, Set.of("Water Stream", "Dispenser", "Piston", "Hopper", "Observer", "Redstone Dust", "Collection System"), 6);
        define(PROFILES[3], all, Set.of("Hopper", "Comparator", "Redstone Torch", "Repeater", "Chest", "Barrel", "Dropper", "Collection System"), 10);
        define(PROFILES[4], all, Set.of("Hopper", "Piston", "Sticky Piston", "Observer", "Dispenser", "Dropper", "Water Stream", "Collection System", "Redstone Dust"), 20);
        define(PROFILES[5], all, Set.of("Chest", "Barrel", "Shulker Box", "Hopper", "Piston", "Observer", "Repeater", "Comparator", "Redstone Dust", "Dispenser"), 3);
        define(PROFILES[6], all, Set.of("Chest", "Hopper"), 1);
    }

    private void define(String profile, List<String> all, Set<String> defaults, int min) {
        String shortName = profile.replace(" Inspection", "");
        MultiSelectSetting ms = add(new MultiSelectSetting(shortName + " Components", "Components counted by the " + profile + " preset.", all, defaults)
                .visibleWhen(() -> active.is(profile)));
        NumberSetting n = add(new NumberSetting(shortName + " Min Count", "Chunks below this total are ignored by the " + profile + " preset.", min, 1, 500, 1)
                .visibleWhen(() -> active.is(profile)));
        components.put(profile, ms);
        minimums.put(profile, n);
    }

    public Set<Component> activeComponents() {
        Set<Component> out = EnumSet.noneOf(Component.class);
        for (String l : components.get(active.get()).get()) {
            Component c = Component.byLabel(l);
            if (c != null) out.add(c);
        }
        return out;
    }

    public int minimum() { return minimums.get(active.get()).getInt(); }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() {
        mc.setScreen(new voidclient.gui.screens.ClickGuiScreen(mc.currentScreen, Category.BASE_HUNTING));
    }

    @Override
    public String getInfo() { return active.get().replace(" Inspection", ""); }
}
