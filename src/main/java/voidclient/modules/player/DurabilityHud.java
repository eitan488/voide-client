package voidclient.modules.player;

import net.minecraft.client.gui.DrawContext;
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

/** Durability of every damageable item you're wearing or holding in the hotbar. */
public class DurabilityHud extends HudModule {
    public final BooleanSetting onlyDamaged = add(new BooleanSetting("Only Damaged", "Hide items at full durability.", true));
    public final ModeSetting display = add(new ModeSetting("Display", "Number format.", "Percent", "Percent", "Remaining", "Both"));
    public final NumberSetting highlight = add(new NumberSetting("Highlight Below", "Red highlight under this durability.", 20, 0, 100, 1).unit("%"));
    public final BooleanSetting includeArmor = add(new BooleanSetting("Include Armor", "List worn armour.", true));

    public DurabilityHud() {
        super("Durability HUD", "Tool and armour durability list with warnings.", Category.PLAYER, 0.005, 0.70);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<ItemStack> list = new ArrayList<>();
        if (mc.player != null) {
            if (includeArmor.get()) for (var slot : InventoryUtil.ARMOR) list.add(mc.player.getEquippedStack(slot));
            for (int i = 0; i < 9; i++) list.add(mc.player.getInventory().getStack(i));
            list.add(mc.player.getOffHandStack());
        }
        list.removeIf(s -> s.isEmpty() || !s.isDamageable() || (onlyDamaged.get() && s.getDamage() == 0));
        if (list.isEmpty() && editor) {
            ItemStack s = new ItemStack(Items.DIAMOND_PICKAXE);
            s.setDamage(900);
            list.add(s);
        }
        if (list.isEmpty()) {
            size(0, 0);
            return;
        }
        float h = list.size() * 18 + 4, w = 0;
        List<String> labels = new ArrayList<>();
        for (ItemStack s : list) {
            int left = s.getMaxDamage() - s.getDamage();
            int pct = Math.round(InventoryUtil.durability(s) * 100);
            String l = switch (display.get()) {
                case "Remaining" -> String.valueOf(left);
                case "Both" -> pct + "% (" + left + ")";
                default -> pct + "%";
            };
            labels.add(l);
            w = Math.max(w, textWidth(l));
        }
        w += 26;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            float y = 2 + i * 18, f = InventoryUtil.durability(s);
            ctx.drawItem(s, 2, (int) y);
            int c = f * 100 < highlight.get() ? 0xFFFF3C3C : ColorUtil.health(f);
            text(ctx, labels.get(i), 21, y + 5, c);
            Render2D.rect(ctx, 21, y + 15, (w - 24) * f, 1.2f, c);
        }
    }
}
