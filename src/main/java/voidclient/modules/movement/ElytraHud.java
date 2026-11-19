package voidclient.modules.movement;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.utilities.InventoryUtil;

import java.util.List;
import java.util.Locale;

/** Flight telemetry while gliding: speed, altitude, pitch, glide ratio, durability, rockets. */
public class ElytraHud extends TextHudModule {
    public final BooleanSetting onlyGliding = add(new BooleanSetting("Only While Gliding", "Hide when not flying.", true));
    public final BooleanSetting rockets = add(new BooleanSetting("Rockets", "Count firework rockets.", true));
    public final BooleanSetting glideRatio = add(new BooleanSetting("Glide Ratio", "Horizontal distance per block of descent.", true));
    public final NumberSetting warnDurability = add(new NumberSetting("Warn Durability", "Red durability below this.", 15, 1, 100, 1).unit("%"));

    private Vec3d last;
    private double speed, vSpeed;
    private double flightDistance;

    public ElytraHud() {
        super("Elytra HUD", "Speed, altitude, pitch and elytra durability while flying.", Category.MOVEMENT, 0.70, 0.40);
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            Vec3d p = mc.player.getEntityPos();
            if (last != null) {
                speed = Math.hypot(p.x - last.x, p.z - last.z) * 20;
                vSpeed = (p.y - last.y) * 20;
                if (mc.player.isGliding()) flightDistance += p.distanceTo(last);
                else if (mc.player.isOnGround()) flightDistance = 0;
            }
            last = p;
        });
    }

    @Override
    public boolean shouldRender() {
        return !onlyGliding.get() || (mc.player != null && mc.player.isGliding());
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("Speed", "33.2 b/s"));
            return;
        }
        var p = mc.player;
        out.add(new Line("Speed", String.format(Locale.ROOT, "%.1f b/s", speed)));
        out.add(new Line("Altitude", String.format(Locale.ROOT, "%.0f", p.getY())));
        out.add(new Line("Pitch", String.format(Locale.ROOT, "%.0f°", p.getPitch())));
        out.add(new Line("Vertical", String.format(Locale.ROOT, "%+.1f b/s", vSpeed)));
        if (glideRatio.get() && vSpeed < -0.05) out.add(new Line("Glide", String.format(Locale.ROOT, "%.1f:1", speed / -vSpeed)));
        out.add(new Line("Flight", String.format(Locale.ROOT, "%.0f m", flightDistance)));
        ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) {
            float f = InventoryUtil.durability(chest);
            int left = chest.getMaxDamage() - chest.getDamage();
            out.add(new Line("Elytra", Math.round(f * 100) + "% (~" + left + "s)", f * 100 < warnDurability.get() ? 0xFFFF3C3C : 0));
        }
        if (rockets.get()) out.add(new Line("Rockets", String.valueOf(InventoryUtil.count(Items.FIREWORK_ROCKET))));
    }
}
