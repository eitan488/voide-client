package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.EntityOutlines;
import voidclient.utilities.EntityGroups;

import java.util.Set;

/** Glowing outlines (vanilla outline shader) on chosen entity groups. */
public class EntityHighlight extends Module implements EntityOutlines.Provider {
    public final MultiSelectSetting groups = add(new MultiSelectSetting("Groups", "Entity groups to outline.", EntityGroups.ALL, Set.of(EntityGroups.PLAYERS, EntityGroups.HOSTILE)));
    public final ColorSetting players = add(new ColorSetting("Players", "Player outline.", 0xFFFF2BD6, false));
    public final ColorSetting hostile = add(new ColorSetting("Hostile", "Hostile outline.", 0xFFFF3C3C, false));
    public final ColorSetting neutral = add(new ColorSetting("Neutral", "Neutral outline.", 0xFFFFA53C, false));
    public final ColorSetting passive = add(new ColorSetting("Passive", "Passive outline.", 0xFF3CFF8C, false));
    public final ColorSetting other = add(new ColorSetting("Other", "Items, projectiles, vehicles, other.", 0xFF00F0FF, false));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Outline range.", 64, 8, 256, 8).unit("m"));

    public EntityHighlight() {
        super("Entity Highlight", "Glowing outlines around entities.", Category.VISUALS);
        EntityOutlines.register(this);
    }

    @Override
    public boolean outlineActive() { return isEnabled(); }

    @Override
    public int outlineColor(Entity e) {
        if (e == mc.player || mc.player == null) return 0;
        String g = EntityGroups.of(e);
        if (!groups.isSelected(g) || e.squaredDistanceTo(mc.player) > maxDistance.get() * maxDistance.get()) return 0;
        return (switch (g) {
            case EntityGroups.PLAYERS -> players.getColor();
            case EntityGroups.HOSTILE -> hostile.getColor();
            case EntityGroups.NEUTRAL -> neutral.getColor();
            case EntityGroups.PASSIVE -> passive.getColor();
            default -> other.getColor();
        }) & 0xFFFFFF | 0x01000000;
    }
}
