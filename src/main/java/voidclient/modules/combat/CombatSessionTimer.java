package voidclient.modules.combat;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.utilities.MathUtil;

import java.util.List;
import java.util.Locale;

/** Combat-tag style timer plus total time spent fighting this session. */
public class CombatSessionTimer extends TextHudModule {
    public final NumberSetting timeout = add(new NumberSetting("Combat Timeout", "Seconds after the last hit before you are out of combat.", 15, 3, 60, 1).unit("s"));
    public final BooleanSetting total = add(new BooleanSetting("Total Combat Time", "Accumulated combat time this session.", true));
    public final BooleanSetting hideOutOfCombat = add(new BooleanSetting("Hide Out Of Combat", "Only show while in combat.", false));

    private long totalMs;
    private long lastTick = System.currentTimeMillis();

    public CombatSessionTimer() {
        super("Combat Session Timer", "In-combat countdown and total fight time.", Category.COMBAT, 0.40, 0.005);
        on(Events.TickPost.class, e -> {
            long now = System.currentTimeMillis();
            if (inCombat()) totalMs += now - lastTick;
            lastTick = now;
        });
    }

    private boolean inCombat() {
        var c = VoidClient.get().combat();
        return c.everInCombat() && c.sinceCombatMs() < timeout.get() * 1000;
    }

    @Override
    public boolean shouldRender() { return !hideOutOfCombat.get() || inCombat(); }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        var c = VoidClient.get().combat();
        if (inCombat()) {
            double left = timeout.get() - c.sinceCombatMs() / 1000.0;
            out.add(new Line("IN COMBAT", String.format(Locale.ROOT, "%.1fs", left), 0xFFFF3C6E));
        } else {
            out.add(new Line("Status", "Safe", 0xFF3CFFA0));
        }
        if (total.get()) out.add(new Line("Combat Time", MathUtil.formatDuration(totalMs)));
    }
}
