package voidclient.modules.combat;

import net.minecraft.entity.LivingEntity;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

import java.util.List;
import java.util.Locale;

/** Reach of your last hit, combo, hits dealt/taken. */
public class CombatInfo extends TextHudModule {
    public final BooleanSetting reach = add(new BooleanSetting("Reach", "Distance of your last landed hit.", true));
    public final BooleanSetting combo = add(new BooleanSetting("Combo", "Hits in a row without taking damage.", true));
    public final BooleanSetting counts = add(new BooleanSetting("Hits Dealt / Taken", "Session hit counts.", true));
    public final NumberSetting comboReset = add(new NumberSetting("Combo Reset", "Combo resets after this long without a hit.", 3, 1, 10, 0.5).unit("s"));

    private double lastReach;
    private int comboCount, hitsDealt, hitsTaken;
    private long lastHitMs;

    public CombatInfo() {
        super("Combat Information", "Reach, combo and hit counters.", Category.COMBAT, 0.38, 0.61);
        on(Events.AttackEntity.class, e -> {
            if (e.target() instanceof LivingEntity && mc.player != null) {
                lastReach = mc.player.getEyePos().distanceTo(e.target().getBoundingBox().getCenter()) - e.target().getWidth() / 2;
            }
        });
        on(Events.HealthChange.class, e -> {
            if (e.newHealth() >= e.oldHealth()) return;
            if (e.entity() == mc.player) {
                hitsTaken++;
                comboCount = 0;
            } else if (e.entity() == VoidClient.get().combat().lastTarget() && VoidClient.get().combat().sinceAttackMs() < 600) {
                hitsDealt++;
                comboCount = System.currentTimeMillis() - lastHitMs > comboReset.get() * 1000 ? 1 : comboCount + 1;
                lastHitMs = System.currentTimeMillis();
            }
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (reach.get()) out.add(new Line("Reach", String.format(Locale.ROOT, "%.2f", Math.max(0, lastReach))));
        if (combo.get()) {
            int c = System.currentTimeMillis() - lastHitMs > comboReset.get() * 1000 ? 0 : comboCount;
            out.add(new Line("Combo", String.valueOf(c), c >= 3 ? accent() : 0));
        }
        if (counts.get()) out.add(new Line("Dealt/Taken", hitsDealt + " / " + hitsTaken));
    }
}
