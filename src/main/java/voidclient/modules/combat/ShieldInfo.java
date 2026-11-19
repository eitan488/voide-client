package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

/** Shield durability, blocking state and the axe-disable cooldown. */
public class ShieldInfo extends HudModule {
    public final BooleanSetting durability = add(new BooleanSetting("Durability", "Show shield durability.", true));
    public final BooleanSetting cooldownBar = add(new BooleanSetting("Cooldown Bar", "Bar while the shield is disabled.", true));
    public final ColorSetting cooldownColor = add(new ColorSetting("Cooldown Color", "Disabled shield colour.", 0xFFFF3C6E));
    public final BooleanSetting hideWithoutShield = add(new BooleanSetting("Hide Without Shield", "Hide when you hold no shield.", true));

    public ShieldInfo() {
        super("Shield Information", "Shield durability, blocking and disable cooldown.", Category.COMBAT, 0.57, 0.80);
    }

    private ItemStack shield() {
        if (mc.player == null) return ItemStack.EMPTY;
        if (mc.player.getOffHandStack().isOf(Items.SHIELD)) return mc.player.getOffHandStack();
        if (mc.player.getMainHandStack().isOf(Items.SHIELD)) return mc.player.getMainHandStack();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean shouldRender() { return !hideWithoutShield.get() || !shield().isEmpty(); }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        ItemStack s = shield();
        if (s.isEmpty()) s = new ItemStack(Items.SHIELD);
        float cd = mc.player == null ? 0 : mc.player.getItemCooldownManager().getCooldownProgress(s, delta);
        boolean blocking = mc.player != null && mc.player.isBlocking();
        String state = cd > 0 ? "DISABLED" : blocking ? "BLOCKING" : "READY";
        String dur = durability.get() ? "  " + Math.round(InventoryUtil.durability(s) * 100) + "%" : "";
        float w = 22 + textWidth(state + dur) + 4, h = 20;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        ctx.drawItem(s, 2, 2);
        text(ctx, state, 20, 6, cd > 0 ? cooldownColor.getColor() : blocking ? accent() : textColor.getColor());
        if (!dur.isEmpty()) text(ctx, dur, 20 + textWidth(state), 6, ColorUtil.health(InventoryUtil.durability(s)));
        if (cooldownBar.get() && cd > 0) Render2D.rect(ctx, 2, h - 2, (w - 4) * cd, 1.5f, cooldownColor.getColor());
    }
}
