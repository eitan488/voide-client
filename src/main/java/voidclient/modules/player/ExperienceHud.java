package voidclient.modules.player;

import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;

import java.util.List;

/** Level, progress and XP needed for the next level. */
public class ExperienceHud extends TextHudModule {
    public final BooleanSetting progress = add(new BooleanSetting("Progress", "Percentage to the next level.", true));
    public final BooleanSetting toNext = add(new BooleanSetting("To Next Level", "XP points still needed.", true));
    public final BooleanSetting total = add(new BooleanSetting("Total XP", "Total experience points.", false));

    public ExperienceHud() {
        super("Experience HUD", "Experience level, progress and points.", Category.PLAYER, 0.80, 0.62);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.player == null) {
            if (editor) out.add(new Line("Level", "30"));
            return;
        }
        var p = mc.player;
        out.add(new Line("Level", String.valueOf(p.experienceLevel)));
        if (progress.get()) out.add(new Line("Progress", Math.round(p.experienceProgress * 100) + "%"));
        if (toNext.get()) out.add(new Line("Next", Math.round((1 - p.experienceProgress) * p.getNextLevelExperience()) + " xp"));
        if (total.get()) out.add(new Line("Total", String.valueOf(p.totalExperience)));
    }
}
