package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.ExplosionImpl;

/**
 * Client-side estimate of explosion damage (crystals power 6, anchors 5, TNT 4) using the vanilla
 * exposure raycast, armour/toughness, Protection/Blast Protection EPF, Resistance and difficulty.
 * It is an estimate: server-side modifiers we cannot see are not included.
 */
public final class ExplosionMath {
    public static final float CRYSTAL = 6f, ANCHOR = 5f, TNT = 4f;

    private ExplosionMath() {}

    public static float estimate(LivingEntity target, Vec3d center, float power) {
        if (target == null || target.getEntityWorld() == null) return 0;
        double dist = Math.sqrt(target.squaredDistanceTo(center)) / (power * 2.0);
        if (dist > 1.0) return 0;
        float exposure = ExplosionImpl.calculateReceivedDamage(center, target);
        double impact = (1.0 - dist) * exposure;
        float dmg = (float) ((impact * impact + impact) / 2.0 * 7.0 * power * 2.0 + 1.0);
        if (target instanceof PlayerEntity) {
            switch (target.getEntityWorld().getDifficulty()) {
                case PEACEFUL -> dmg = 0;
                case EASY -> dmg = Math.min(dmg / 2f + 1f, dmg);
                case HARD -> dmg = dmg * 1.5f;
                default -> {
                }
            }
        }
        dmg = armor(dmg, target.getArmor(), (float) target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
        var res = target.getStatusEffect(StatusEffects.RESISTANCE);
        if (res != null) dmg *= Math.max(0f, 1f - (res.getAmplifier() + 1) * 0.2f);
        dmg = dmg * (1f - Math.min(20, epf(target)) / 25f);
        return Math.max(0f, dmg);
    }

    /** Vanilla armour formula without the weapon-enchantment hook (pure). */
    public static float armor(float damage, float armor, float toughness) {
        float f = 2f + toughness / 4f;
        float g = Math.max(armor * 0.2f, Math.min(20f, armor - damage / f));
        return damage * (1f - g / 25f);
    }

    private static int epf(LivingEntity e) {
        var world = MinecraftClient.getInstance().world;
        if (world == null) return 0;
        var reg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> prot = entry(reg, Enchantments.PROTECTION), blast = entry(reg, Enchantments.BLAST_PROTECTION);
        int total = 0;
        for (var slot : InventoryUtil.ARMOR) {
            ItemStack s = e.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            if (prot != null) total += EnchantmentHelper.getLevel(prot, s);
            if (blast != null) total += 2 * EnchantmentHelper.getLevel(blast, s);
        }
        return total;
    }

    private static RegistryEntry<Enchantment> entry(net.minecraft.registry.Registry<Enchantment> reg, RegistryKey<Enchantment> key) {
        return reg.getOptional(key).orElse(null);
    }
}
