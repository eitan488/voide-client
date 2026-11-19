package voidclient.modules.movement;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

import java.util.List;
import java.util.Locale;

/** Steps and distance travelled this session, split by movement type. */
public class StepStatistics extends TextHudModule {
    public final NumberSetting stepLength = add(new NumberSetting("Step Length", "Blocks per step used for the step count.", 0.75, 0.3, 1.5, 0.05).unit("m"));
    public final ModeSetting units = add(new ModeSetting("Units", "Distance unit.", "m", "m", "km"));
    public final BooleanSetting breakdown = add(new BooleanSetting("Breakdown", "Walk / sprint / swim / fly / elytra / ride.", true));
    public final BooleanSetting jumps = add(new BooleanSetting("Jumps", "Jump count.", true));

    public StepStatistics() {
        super("Step Statistics", "Step count and travel distance by movement type.", Category.MOVEMENT, 0.005, 0.60);
    }

    private String d(double m) {
        return units.is("km") ? String.format(Locale.ROOT, "%.2f km", m / 1000) : String.format(Locale.ROOT, "%.0f m", m);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        var s = VoidClient.get().session();
        long steps = Math.round((s.walked() + s.sprinted()) / stepLength.get());
        out.add(new Line("Steps", String.valueOf(steps)));
        out.add(new Line("Distance", d(s.totalDistance())));
        if (breakdown.get()) {
            if (s.walked() > 0 || editor) out.add(new Line("Walk", d(s.walked())));
            if (s.sprinted() > 0 || editor) out.add(new Line("Sprint", d(s.sprinted())));
            if (s.swum() > 0) out.add(new Line("Swim", d(s.swum())));
            if (s.flown() > 0) out.add(new Line("Fly", d(s.flown())));
            if (s.elytra() > 0) out.add(new Line("Elytra", d(s.elytra())));
            if (s.ridden() > 0) out.add(new Line("Ride", d(s.ridden())));
        }
        if (jumps.get()) out.add(new Line("Jumps", String.valueOf(s.jumps())));
    }
}
