package voidclient.modules.movement;

import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.events.Events;

import java.util.List;
import java.util.Locale;

/** Speed, vertical speed and movement state. */
public class MovementHud extends TextHudModule {
    public final ModeSetting units = add(new ModeSetting("Units", "Speed unit.", "b/s", "b/s", "km/h", "mph"));
    public final BooleanSetting vertical = add(new BooleanSetting("Vertical Speed", "Show vertical speed.", true));
    public final BooleanSetting state = add(new BooleanSetting("State", "Walking, sprinting, sneaking, swimming, flying...", true));
    public final BooleanSetting peak = add(new BooleanSetting("Peak Speed", "Highest speed in the last minute.", false));

    private Vec3d last;
    private double speed, vSpeed, peakSpeed;
    private int peakAge;

    public MovementHud() {
        super("Movement HUD", "Speed and movement state readout.", Category.MOVEMENT, 0.005, 0.20);
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            Vec3d p = mc.player.getEntityPos();
            if (last != null) {
                speed = Math.hypot(p.x - last.x, p.z - last.z) * 20;
                vSpeed = (p.y - last.y) * 20;
                if (speed > peakSpeed || ++peakAge > 1200) {
                    peakSpeed = speed;
                    peakAge = 0;
                }
            }
            last = p;
        });
    }

    private String fmt(double bps) {
        return switch (units.get()) {
            case "km/h" -> String.format(Locale.ROOT, "%.1f km/h", bps * 3.6);
            case "mph" -> String.format(Locale.ROOT, "%.1f mph", bps * 2.23694);
            default -> String.format(Locale.ROOT, "%.2f b/s", bps);
        };
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("Speed", fmt(5.61)));
            return;
        }
        out.add(new Line("Speed", fmt(speed)));
        if (vertical.get()) out.add(new Line("Vertical", fmt(vSpeed)));
        if (peak.get()) out.add(new Line("Peak", fmt(peakSpeed)));
        if (state.get()) {
            var p = mc.player;
            String s = p.isGliding() ? "Gliding" : p.getAbilities().flying ? "Flying" : p.isSwimming() ? "Swimming" : p.hasVehicle() ? "Riding"
                    : p.isSneaking() ? "Sneaking" : p.isSprinting() ? "Sprinting" : speed > 0.1 ? "Walking" : "Idle";
            out.add(new Line("State", s + (p.isOnGround() ? "" : " (air)")));
        }
    }
}
