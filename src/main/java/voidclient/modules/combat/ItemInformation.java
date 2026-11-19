package voidclient.modules.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.utilities.InventoryUtil;

import java.util.List;

/** Name, enchantments, durability and stock of the item in your hand. */
public class ItemInformation extends TextHudModule {
    public final BooleanSetting enchants = add(new BooleanSetting("Enchantments", "List enchantments.", true));
    public final NumberSetting maxEnchants = add(new NumberSetting("Max Enchantments", "Max listed enchantments.", 5, 1, 12, 1).visibleWhen(enchants::get));
    public final BooleanSetting stock = add(new BooleanSetting("Stock", "Total of this item in your inventory.", true));
    public final BooleanSetting durability = add(new BooleanSetting("Durability", "Remaining durability.", true));

    public ItemInformation() {
        super("Item Information", "Details about the item you are holding.", Category.COMBAT, 0.62, 0.70);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack();
        if (s.isEmpty()) {
            if (editor) out.add(new Line("Item", "Netherite Sword"));
            return;
        }
        int rarity = s.getRarity().getFormatting().getColorValue() == null ? 0 : 0xFF000000 | s.getRarity().getFormatting().getColorValue();
        out.add(new Line("", s.getName().getString(), rarity));
        if (durability.get() && s.isDamageable()) {
            out.add(new Line("Durability", (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage() + " (" + Math.round(InventoryUtil.durability(s) * 100) + "%)"));
        }
        if (stock.get() && s.isStackable()) out.add(new Line("Stock", String.valueOf(InventoryUtil.count(s.getItem()))));
        if (enchants.get()) {
            ItemEnchantmentsComponent ench = s.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            int n = 0;
            for (RegistryEntry<Enchantment> e : ench.getEnchantments()) {
                if (n++ >= maxEnchants.getInt()) break;
                out.add(new Line("", Enchantment.getName(e, ench.getLevel(e)).getString(), 0xFFB9A0FF));
            }
        }
    }
}
