package voidclient.modules.combat;

import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.events.Events;

import java.util.List;
import java.util.Locale;

/** Swings, hits, accuracy, critical hits, best combo and damage dealt (from health changes). */
public class AttackStatistics extends TextHudModule {
    public final BooleanSetting accuracy = add(new BooleanSetting("Accuracy", "Landed hits / swings.", true));
    public final BooleanSetting crits = add(new BooleanSetting("Critical Hits", "Attacks made while falling (vanilla crit rules).", true));
    public final BooleanSetting damage = add(new BooleanSetting("Damage Dealt", "Sum of health your targets lost.", true));
    public final BooleanSetting bestCombo = add(new BooleanSetting("Best Combo", "Longest streak of hits without being hit.", true));
    public final BooleanSetting resetNow = add(new BooleanSetting("Reset Statistics", "Turn on to reset (switches itself off).", false));

    private int swings, hits, critCount, combo, best;
    private float dealt;

    public AttackStatistics() {
        super("Attack Statistics", "Swing accuracy, crits, combos and damage dealt.", Category.COMBAT, 0.60, 0.005);
        resetNow.onChange(v -> {
            if (v) {
                swings = hits = critCount = combo = best = 0;
                dealt = 0;
                resetNow.set(false);
            }
        });
        on(Events.MouseButton.class, e -> {
            if (e.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && e.action == GLFW.GLFW_PRESS && mc.currentScreen == null && mc.player != null
                    && mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.BLOCK) swings++;
        });
        on(Events.AttackEntity.class, e -> {
            var p = mc.player;
            if (p != null && p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater() && !p.hasVehicle()
                    && !p.isSprinting() && p.getAttackCooldownProgress(0.5f) > 0.9f) critCount++;
        });
        on(Events.HealthChange.class, e -> {
            var c = VoidClient.get().combat();
            if (e.newHealth() >= e.oldHealth()) return;
            if (e.entity() == mc.player) {
                combo = 0;
            } else if (e.entity() == c.lastTarget() && c.sinceAttackMs() < 600) {
                hits++;
                dealt += e.oldHealth() - e.newHealth();
                best = Math.max(best, ++combo);
            }
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        out.add(new Line("Hits", hits + " / " + swings));
        if (accuracy.get()) out.add(new Line("Accuracy", swings == 0 ? "-" : String.format(Locale.ROOT, "%.0f%%", 100.0 * Math.min(hits, swings) / swings)));
        if (crits.get()) out.add(new Line("Crits", String.valueOf(critCount)));
        if (damage.get()) out.add(new Line("Damage", String.format(Locale.ROOT, "%.1f", dealt)));
        if (bestCombo.get()) out.add(new Line("Best Combo", String.valueOf(best)));
    }
}
