package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Screen-edge flash when you take damage and a pulse at low health. */
public class DamageOverlay extends Module {
    public final ModeSetting style = add(new ModeSetting("Style", "Edge vignette or full screen tint.", "Edges", "Edges", "Full Screen"));
    public final ColorSetting color = add(new ColorSetting("Color", "Flash colour.", 0xB0FF1030));
    public final NumberSetting duration = add(new NumberSetting("Duration", "Flash length.", 450, 100, 2000, 50).unit("ms"));
    public final BooleanSetting lowHealthPulse = add(new BooleanSetting("Low Health Pulse", "Pulse while health is low.", true));
    public final NumberSetting lowHealth = add(new NumberSetting("Low Health", "Hearts that count as low.", 4, 1, 10, 0.5).visibleWhen(lowHealthPulse::get));

    private long hitTime;

    public DamageOverlay() {
        super("Damage Overlay", "Red screen flash when hurt and a low-health pulse.", Category.VISUALS);
        on(Events.HealthChange.class, e -> {
            if (e.entity() == mc.player && e.newHealth() < e.oldHealth()) hitTime = System.currentTimeMillis();
        });
        on(Events.HudRender.class, e -> {
            if (mc.player == null) return;
            float a = Math.max(0, 1f - (System.currentTimeMillis() - hitTime) / duration.getFloat());
            if (lowHealthPulse.get() && mc.player.getHealth() <= lowHealth.get() * 2 && mc.player.isAlive()) {
                a = Math.max(a, (float) (0.35 + 0.25 * Math.sin(System.currentTimeMillis() / 220.0)));
            }
            if (a <= 0.01f) return;
            int c = ColorUtil.multiplyAlpha(color.getColor(), a);
            if (style.is("Full Screen")) {
                Render2D.rect(e.ctx, 0, 0, e.width, e.height, ColorUtil.multiplyAlpha(c, 0.4f));
                return;
            }
            float edge = Math.min(e.width, e.height) * 0.18f;
            Render2D.gradientV(e.ctx, 0, 0, e.width, edge, c, 0);
            Render2D.gradientV(e.ctx, 0, e.height - edge, e.width, edge, 0, c);
            Render2D.gradientH(e.ctx, 0, 0, edge, e.height, c, 0);
            Render2D.gradientH(e.ctx, e.width - edge, 0, edge, e.height, 0, c);
        });
    }
}
