package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Health bars floating above living entities. */
public class HealthBars extends Module {
    public final ModeSetting targets = add(new ModeSetting("Targets", "Who gets a bar.", "All Living", "Players", "Mobs", "All Living"));
    public final NumberSetting width = add(new NumberSetting("Width", "Bar width in blocks.", 0.9, 0.3, 2, 0.05));
    public final NumberSetting offset = add(new NumberSetting("Height Offset", "Height above the head.", 0.35, 0, 1.5, 0.05));
    public final BooleanSetting number = add(new BooleanSetting("Number", "Health number above the bar.", true));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Bar range.", 48, 8, 256, 4).unit("m"));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw through blocks.", false));

    public HealthBars() {
        super("Health Bars", "Health bars above players and mobs.", Category.VISUALS);
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null) return;
            var cam = mc.gameRenderer.getCamera();
            for (Entity ent : mc.world.getEntities()) {
                if (!(ent instanceof LivingEntity le) || ent == mc.player || ent instanceof ArmorStandEntity || !le.isAlive()) continue;
                boolean isPlayer = ent instanceof PlayerEntity;
                if (targets.is("Players") && !isPlayer || targets.is("Mobs") && isPlayer) continue;
                if (ent.squaredDistanceTo(mc.player) > maxDistance.get() * maxDistance.get()) continue;
                Vec3d top = ent.getLerpedPos(e.tickDelta).add(0, ent.getHeight() + offset.get(), 0);
                float f = Math.min(1f, le.getHealth() / Math.max(1f, le.getMaxHealth()));
                double w = width.get();
                double yawRad = Math.toRadians(-cam.getYaw());
                double cx = Math.cos(yawRad) * w / 2, cz = Math.sin(yawRad) * w / 2;
                Vec3d left = top.add(-cx, 0, -cz), right = top.add(cx, 0, cz);
                RenderManager.line(left, right, 0xC0000000, 7f, throughWalls.get());
                Vec3d fillEnd = left.add((right.x - left.x) * f, 0, (right.z - left.z) * f);
                RenderManager.line(left, fillEnd, ColorUtil.health(f), 5f, throughWalls.get());
                if (number.get()) RenderManager.text(Text.literal(String.format(Locale.ROOT, "%.1f", le.getHealth() + le.getAbsorptionAmount())),
                        top.add(0, 0.12, 0), 0.15f, ColorUtil.health(f), 0, throughWalls.get(), true);
            }
        });
    }

}
