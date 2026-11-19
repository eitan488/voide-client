package voidclient.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Combat indicators around the crosshair: attack cooldown arc, in-reach dot and hit marker. */
public class CombatCrosshair extends Module {
    public final BooleanSetting cooldownArc = add(new BooleanSetting("Cooldown Arc", "Ring that fills as your attack recharges.", true));
    public final NumberSetting arcRadius = add(new NumberSetting("Arc Radius", "Radius of the cooldown ring.", 9, 4, 24, 0.5).visibleWhen(cooldownArc::get));
    public final ColorSetting arcColor = add(new ColorSetting("Arc Color", "Cooldown ring colour.", 0xCC00F0FF).themed().visibleWhen(cooldownArc::get));
    public final BooleanSetting reachIndicator = add(new BooleanSetting("Reach Indicator", "Dot when a living target is under the crosshair.", true));
    public final ColorSetting reachColor = add(new ColorSetting("Reach Color", "Reach dot colour.", 0xFFFF3C6E).visibleWhen(reachIndicator::get));
    public final BooleanSetting hitMarker = add(new BooleanSetting("Hit Marker", "X marker when your hit lands.", true));
    public final ColorSetting hitColor = add(new ColorSetting("Hit Marker Color", "Hit marker colour.", 0xFFFFFFFF).visibleWhen(hitMarker::get));
    public final NumberSetting hitDuration = add(new NumberSetting("Hit Marker Duration", "How long the marker stays.", 250, 50, 1000, 10).unit("ms").visibleWhen(hitMarker::get));
    public final NumberSetting hitSize = add(new NumberSetting("Hit Marker Size", "Marker size.", 5, 2, 12, 0.5).visibleWhen(hitMarker::get));

    private long lastHit;

    public CombatCrosshair() {
        super("Crosshair", "Combat crosshair indicators: cooldown arc, reach dot and hit markers.", Category.COMBAT);
        on(Events.HealthChange.class, e -> {
            var c = VoidClient.get().combat();
            if (e.newHealth() < e.oldHealth() && e.entity() == c.lastTarget() && c.sinceAttackMs() < 600) lastHit = System.currentTimeMillis();
        });
        on(Events.HudRender.class, e -> {
            if (e.hudHidden || mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;
            float cx = e.width / 2f, cy = e.height / 2f;
            if (cooldownArc.get()) {
                float p = mc.player.getAttackCooldownProgress(0f);
                if (p < 1f) {
                    Render2D.ring(e.ctx, cx, cy, arcRadius.getFloat(), 1.5f, 0x40000000);
                    Render2D.arc(e.ctx, cx, cy, arcRadius.getFloat(), 1.5f, 0, 360 * p, arcColor.getColor());
                }
            }
            if (reachIndicator.get() && mc.crosshairTarget instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity) {
                Render2D.circle(e.ctx, cx, cy + arcRadius.getFloat() + 4, 1.6f, reachColor.getColor());
            }
            long since = System.currentTimeMillis() - lastHit;
            if (hitMarker.get() && since < hitDuration.get()) {
                float a = 1f - since / hitDuration.getFloat();
                int c = ColorUtil.multiplyAlpha(hitColor.getColor(), a);
                float s = hitSize.getFloat(), g = 3;
                Render2D.line(e.ctx, cx - g - s, cy - g - s, cx - g, cy - g, 1.3f, c);
                Render2D.line(e.ctx, cx + g + s, cy - g - s, cx + g, cy - g, 1.3f, c);
                Render2D.line(e.ctx, cx - g - s, cy + g + s, cx - g, cy + g, 1.3f, c);
                Render2D.line(e.ctx, cx + g + s, cy + g + s, cx + g, cy + g, 1.3f, c);
            }
        });
    }
}
