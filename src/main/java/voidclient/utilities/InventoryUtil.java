package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Read-only inventory queries (counts, armor, durability). */
public final class InventoryUtil {
    public static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private InventoryUtil() {}

    private static PlayerEntity player() { return MinecraftClient.getInstance().player; }

    /** Counts an item across main inventory, hotbar and offhand. */
    public static int count(Item item) {
        PlayerEntity p = player();
        if (p == null) return 0;
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    public static int countHotbar(Item item) {
        PlayerEntity p = player();
        if (p == null) return 0;
        int n = 0;
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(item)) n += p.getInventory().getStack(i).getCount();
        return n;
    }

    public static Item item(String id) {
        Identifier ident = Identifier.tryParse(id.trim());
        if (ident == null || !Registries.ITEM.containsId(ident)) return null;
        return Registries.ITEM.get(ident);
    }

    public static List<Item> items(String csv) {
        List<Item> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            if (s.isBlank()) continue;
            Item it = item(s.contains(":") ? s : "minecraft:" + s.trim());
            if (it != null) out.add(it);
        }
        return out;
    }

    public static ItemStack armor(EquipmentSlot slot) {
        PlayerEntity p = player();
        return p == null ? ItemStack.EMPTY : p.getEquippedStack(slot);
    }

    /** Remaining durability fraction (1 = new). Non-damageable stacks return 1. */
    public static float durability(ItemStack s) {
        if (s.isEmpty() || !s.isDamageable() || s.getMaxDamage() <= 0) return 1f;
        return 1f - s.getDamage() / (float) s.getMaxDamage();
    }

    /** Lowest durability fraction among worn armour. */
    public static float lowestArmor() {
        float min = 1f;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = armor(slot);
            if (!s.isEmpty() && s.isDamageable()) min = Math.min(min, durability(s));
        }
        return min;
    }

    public static String formatCount(int n, boolean stacks) {
        if (!stacks || n < 64) return String.valueOf(n);
        int st = n / 64, rem = n % 64;
        return rem == 0 ? st + "s" : st + "s+" + rem;
    }
}
