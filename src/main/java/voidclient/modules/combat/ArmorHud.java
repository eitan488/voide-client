package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

/** Worn armour (and optionally held items) with durability. */
public class ArmorHud extends HudModule {
    public final ModeSetting layout = add(new ModeSetting("Layout", "Arrangement.", "Vertical", "Vertical", "Horizontal"));
    public final ModeSetting durability = add(new ModeSetting("Durability", "Durability display.", "Percent", "None", "Percent", "Remaining", "Bar"));
    public final BooleanSetting mainHand = add(new BooleanSetting("Main Hand", "Include the held item.", true));
    public final BooleanSetting offHand = add(new BooleanSetting("Off Hand", "Include the off-hand item.", true));
    public final NumberSetting warnBelow = add(new NumberSetting("Warn Below", "Flash pieces under this durability.", 15, 0, 100, 1).unit("%"));

    public ArmorHud() {
        super("Armor HUD", "Your armour pieces and their durability.", Category.COMBAT, 0.165, 0.455);
        alsoListIn(Category.VISUALS);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<ItemStack> items = new ArrayList<>();
        if (mc.player != null) {
            for (int i = InventoryUtil.ARMOR.length - 1; i >= 0; i--) items.add(0, mc.player.getEquippedStack(InventoryUtil.ARMOR[i]));
            if (mainHand.get()) items.add(mc.player.getMainHandStack());
            if (offHand.get()) items.add(mc.player.getEquippedStack(EquipmentSlot.OFFHAND));
        }
        items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty() && editor) {
            items.add(new ItemStack(Items.NETHERITE_HELMET));
            items.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
        }
        if (items.isEmpty()) {
            size(0, 0);
            return;
        }
        boolean vertical = layout.is("Vertical");
        boolean showText = durability.is("Percent") || durability.is("Remaining");
        float textW = showText ? textWidth("100%") + 4 : 0;
        float cellW = vertical ? 20 + textW : 20 + (showText ? Math.max(0, textW - 16) : 0), cellH = vertical ? 18 : 18 + (showText ? 10 : 0);
        float w = vertical ? cellW + 4 : items.size() * cellW + 4, h = vertical ? items.size() * cellH + 4 : cellH + 4;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            float x = vertical ? 2 : 2 + i * cellW, y = vertical ? 2 + i * cellH : 2;
            float f = InventoryUtil.durability(s);
            boolean warn = s.isDamageable() && f * 100 < warnBelow.get();
            if (warn && blink) Render2D.roundRect(ctx, x, y, 16, 16, 2, 0x60FF2020);
            ctx.drawItem(s, (int) x, (int) y);
            if (s.isStackable() && s.getCount() > 1) ctx.drawStackOverlay(mc.textRenderer, s, (int) x, (int) y);
            if (!s.isDamageable()) continue;
            int c = ColorUtil.health(f);
            switch (durability.get()) {
                case "Percent", "Remaining" -> {
                    String t = durability.is("Percent") ? Math.round(f * 100) + "%" : String.valueOf(s.getMaxDamage() - s.getDamage());
                    if (vertical) text(ctx, t, x + 19, y + 5, c);
                    else text(ctx, t, x + 8 - textWidth(t) / 2f, y + 18, c);
                }
                case "Bar" -> Render2D.rect(ctx, x + 1, y + 15, 14 * f, 1.5f, c);
                default -> {
                }
            }
        }
    }
}
