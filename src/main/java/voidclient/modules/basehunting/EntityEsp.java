package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.utilities.EntityGroups;

import java.util.Set;

/** Mobs and other entities by group. */
public class EntityEsp extends EntityEspBase {
    public final MultiSelectSetting groups = add(new MultiSelectSetting("Groups", "Entity groups to show.", EntityGroups.ALL, Set.of(EntityGroups.HOSTILE, EntityGroups.NEUTRAL)));

    public EntityEsp() {
        super("Entity ESP", "Highlights mobs and other entities by group.");
        color.defaultTo(new voidclient.core.setting.ColorSetting.ColorValue(0xFFFF3C3C, false, false));
    }

    @Override
    protected boolean accepts(Entity e) {
        return !(e instanceof PlayerEntity) && groups.isSelected(EntityGroups.of(e));
    }

    @Override
    protected int colorFor(Entity e) {
        return switch (EntityGroups.of(e)) {
            case EntityGroups.PASSIVE -> 0xFF3CFF8C;
            case EntityGroups.NEUTRAL -> 0xFFFFA53C;
            case EntityGroups.HOSTILE -> color.getColor();
            default -> 0xFF00F0FF;
        };
    }
}
