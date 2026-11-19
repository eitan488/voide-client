package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;

import java.util.List;
import java.util.Locale;

/** Details of the entity under your crosshair. */
public class EntityInformation extends TextHudModule {
    public final BooleanSetting typeId = add(new BooleanSetting("Type ID", "Registry id.", true));
    public final BooleanSetting health = add(new BooleanSetting("Health", "Health and armour.", true));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", "Distance from you.", true));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", "Current speed.", false));
    public final BooleanSetting owner = add(new BooleanSetting("Owner / Age", "Tamed owner and baby status.", true));

    public EntityInformation() {
        super("Entity Information", "Type, health, distance and owner of the targeted entity.", Category.VISUALS, 0.40, 0.18);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || mc.player == null) {
            if (editor) out.add(new Line("", "Zombie"));
            return;
        }
        Entity e = hit.getEntity();
        out.add(new Line("", e.getName().getString(), accent()));
        if (typeId.get()) out.add(new Line("Type", Registries.ENTITY_TYPE.getId(e.getType()).toString()));
        if (health.get() && e instanceof LivingEntity le) {
            out.add(new Line("Health", String.format(Locale.ROOT, "%.1f / %.0f", le.getHealth(), le.getMaxHealth())));
            if (le.getArmor() > 0) out.add(new Line("Armor", String.valueOf(le.getArmor())));
        }
        if (distance.get()) out.add(new Line("Distance", String.format(Locale.ROOT, "%.2f m", e.distanceTo(mc.player))));
        if (speed.get()) out.add(new Line("Speed", String.format(Locale.ROOT, "%.2f b/s", e.getVelocity().horizontalLength() * 20)));
        if (owner.get()) {
            if (e instanceof TameableEntity t && t.isTamed()) out.add(new Line("Tamed", "yes"));
            if (e instanceof PassiveEntity p && p.isBaby()) out.add(new Line("Age", "Baby"));
        }
    }
}
