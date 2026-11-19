package voidclient.modules.render;

import net.minecraft.entity.Entity;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.utilities.EntityGroups;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Counts loaded entities by group within a radius. */
public class EntityCounter extends TextHudModule {
    public final NumberSetting radius = add(new NumberSetting("Radius", "Count entities within this distance (0 = all loaded).", 0, 0, 256, 8).unit("m"));
    public final MultiSelectSetting groups = add(new MultiSelectSetting("Groups", "Groups to list.", EntityGroups.ALL, Set.of(EntityGroups.PLAYERS, EntityGroups.HOSTILE, EntityGroups.PASSIVE, EntityGroups.ITEMS)));
    public final BooleanSetting total = add(new BooleanSetting("Total", "Show the total count.", true));

    public EntityCounter() {
        super("Entity Counter", "Loaded entity counts per group.", Category.RENDER, 0.005, 0.48);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.world == null || mc.player == null) {
            if (editor) out.add(new Line("Entities", "42"));
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String g : EntityGroups.ALL) counts.put(g, 0);
        int all = 0;
        double r2 = radius.get() * radius.get();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (r2 > 0 && e.squaredDistanceTo(mc.player) > r2) continue;
            counts.merge(EntityGroups.of(e), 1, Integer::sum);
            all++;
        }
        if (total.get()) out.add(new Line("Entities", String.valueOf(all)));
        for (String g : EntityGroups.ALL) if (groups.isSelected(g)) out.add(new Line(g, String.valueOf(counts.get(g))));
    }
}
