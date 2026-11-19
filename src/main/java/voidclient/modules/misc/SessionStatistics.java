package voidclient.modules.misc;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.utilities.MathUtil;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Session playtime, distance, combat and mining counters. */
public class SessionStatistics extends TextHudModule {
    public final MultiSelectSetting shown = add(new MultiSelectSetting("Shown", "Statistics to display.",
            List.of("Session Time", "World Time", "Distance", "Jumps", "Attacks", "Blocks Mined", "Deaths", "Totem Pops"),
            Set.of("Session Time", "World Time", "Distance", "Attacks", "Blocks Mined")));

    public SessionStatistics() {
        super("Session Statistics", "Playtime, distance, attacks, mining and deaths this session.", Category.MISC, 0.005, 0.66);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        var s = VoidClient.get().session();
        if (shown.isSelected("Session Time")) out.add(new Line("Session", MathUtil.formatDuration(s.sessionMs())));
        if (shown.isSelected("World Time")) out.add(new Line("This World", MathUtil.formatDuration(s.worldMs())));
        if (shown.isSelected("Distance")) out.add(new Line("Distance", String.format(Locale.ROOT, "%.0f m", s.totalDistance())));
        if (shown.isSelected("Jumps")) out.add(new Line("Jumps", String.valueOf(s.jumps())));
        if (shown.isSelected("Attacks")) out.add(new Line("Attacks", String.valueOf(s.attacks())));
        if (shown.isSelected("Blocks Mined")) out.add(new Line("Mined", String.valueOf(s.blocksMined())));
        if (shown.isSelected("Deaths")) out.add(new Line("Deaths", String.valueOf(s.deaths())));
        if (shown.isSelected("Totem Pops")) out.add(new Line("Pops", String.valueOf(s.totemPops())));
    }
}
