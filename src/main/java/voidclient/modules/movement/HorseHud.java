package voidclient.modules.movement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;

import java.util.List;
import java.util.Locale;

/** Stats of the mount you're riding: health, max speed, jump height. */
public class HorseHud extends TextHudModule {
    public final BooleanSetting health = add(new BooleanSetting("Health", "Mount health.", true));
    public final BooleanSetting maxSpeed = add(new BooleanSetting("Max Speed", "Top speed from the movement attribute.", true));
    public final BooleanSetting jump = add(new BooleanSetting("Jump Height", "Max jump height from the jump attribute.", true));
    public final BooleanSetting currentSpeed = add(new BooleanSetting("Current Speed", "Actual speed right now.", true));

    public HorseHud() {
        super("Horse HUD", "Mount health, speed and jump statistics.", Category.MOVEMENT, 0.70, 0.30);
    }

    @Override
    public boolean shouldRender() { return mc.player != null && mc.player.getVehicle() instanceof LivingEntity; }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        Entity v = mc.player == null ? null : mc.player.getVehicle();
        if (!(v instanceof LivingEntity mount)) {
            if (editor) out.add(new Line("Mount", "Horse"));
            return;
        }
        out.add(new Line("Mount", mount.getName().getString()));
        if (health.get()) out.add(new Line("Health", String.format(Locale.ROOT, "%.1f / %.0f", mount.getHealth(), mount.getMaxHealth())));
        if (maxSpeed.get() && mount.getAttributes().hasAttribute(EntityAttributes.MOVEMENT_SPEED)) {
            out.add(new Line("Max Speed", String.format(Locale.ROOT, "%.2f b/s", mount.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 42.157)));
        }
        if (jump.get() && mount instanceof AbstractHorseEntity && mount.getAttributes().hasAttribute(EntityAttributes.JUMP_STRENGTH)) {
            double js = mount.getAttributeValue(EntityAttributes.JUMP_STRENGTH);
            double height = -0.1817584952 * js * js * js + 3.689713992 * js * js + 2.128599134 * js - 0.343930367;
            out.add(new Line("Jump", String.format(Locale.ROOT, "%.2f blocks", height)));
        }
        if (currentSpeed.get()) out.add(new Line("Speed", String.format(Locale.ROOT, "%.2f b/s", mount.getVelocity().horizontalLength() * 20)));
    }
}
