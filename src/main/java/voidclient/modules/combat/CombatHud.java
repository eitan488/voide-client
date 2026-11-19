package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Compact combat panel: your health/absorption/armour vs. your target. */
public class CombatHud extends HudModule {
    public final BooleanSetting showTarget = add(new BooleanSetting("Target Row", "Second row with the target's health.", true));
    public final BooleanSetting armorPoints = add(new BooleanSetting("Armor Points", "Show armour and toughness points.", true));
    public final BooleanSetting bars = add(new BooleanSetting("Bars", "Draw health bars.", true));

    public CombatHud() {
        super("Combat HUD", "Compact you-vs-target health panel.", Category.COMBAT, 0.40, 0.70);
    }

    private void row(DrawContext ctx, float y, String name, LivingEntity e, float w) {
        float hp = e.getHealth(), abs = e.getAbsorptionAmount(), max = Math.max(1, e.getMaxHealth());
        text(ctx, name, 4, y, textColor.getColor());
        String v = String.format(Locale.ROOT, "%.1f", hp + abs);
        text(ctx, v, w - 4 - textWidth(v), y, ColorUtil.health(hp / max));
        if (bars.get()) {
            Render2D.roundRect(ctx, 4, y + 10, w - 8, 3, 1.5f, 0x60000000);
            Render2D.roundRect(ctx, 4, y + 10, (w - 8) * Math.min(1, hp / max), 3, 1.5f, ColorUtil.health(hp / max));
            if (abs > 0) Render2D.roundRect(ctx, 4, y + 10, (w - 8) * Math.min(1, abs / max), 1.5f, 0.75f, 0xFFFFD700);
        }
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        if (mc.player == null) {
            size(110, 20);
            if (editor) panel(ctx, 0, 0, 110, 20);
            return;
        }
        LivingEntity target = showTarget.get() ? VoidClient.get().combat().lastTarget() : null;
        float w = 120, h = 20 + (target != null ? 18 : 0) + (armorPoints.get() ? 10 : 0);
        size(w, h);
        panel(ctx, 0, 0, w, h);
        row(ctx, 3, "You", mc.player, w);
        float y = 21;
        if (armorPoints.get()) {
            text(ctx, String.format(Locale.ROOT, "Armor %d  Tough %.0f", mc.player.getArmor(),
                    mc.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS)), 4, y - 2, dim(textColor.getColor()));
            y += 10;
        }
        if (target != null) row(ctx, y, target.getName().getString(), target, w);
    }
}
