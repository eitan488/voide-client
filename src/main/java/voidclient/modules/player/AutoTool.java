package voidclient.modules.player;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Selects the fastest hotbar tool for the block you start mining. */
public class AutoTool extends Module {
    public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Return to the previous slot after mining.", true));
    public final NumberSetting switchBackDelay = add(new NumberSetting("Switch Back Delay", "Ticks without mining before switching back.", 6, 1, 40, 1).visibleWhen(switchBack::get));
    public final BooleanSetting protectDurability = add(new BooleanSetting("Protect Durability", "Skip tools below the minimum durability.", true));
    public final NumberSetting minDurability = add(new NumberSetting("Min Durability", "Remaining uses below which a tool is skipped.", 10, 1, 200, 1).visibleWhen(protectDurability::get));
    public final BooleanSetting useEnchants = add(new BooleanSetting("Consider Efficiency", "Prefer tools with Efficiency.", true));
    public final BooleanSetting creativeOff = add(new BooleanSetting("Disable In Creative", "Do nothing in creative mode.", true));

    private int previousSlot = -1;
    private int idleTicks;

    public AutoTool() {
        super("Auto Tool", "Automatically switches to the best tool when you mine.", Category.PLAYER);
        on(Events.AttackBlock.class, e -> select(mc.world.getBlockState(e.pos())));
        on(Events.TickPost.class, e -> {
            if (mc.player == null || previousSlot < 0) return;
            if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
                idleTicks = 0;
                return;
            }
            if (++idleTicks >= switchBackDelay.getInt()) {
                if (switchBack.get()) mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        });
    }

    private double score(ItemStack stack, BlockState state) {
        if (protectDurability.get() && stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() < minDurability.get()) return -1;
        double speed = stack.getMiningSpeedMultiplier(state);
        if (useEnchants.get() && speed > 1 && mc.world != null) {
            RegistryEntry<Enchantment> eff = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
            int lvl = EnchantmentHelper.getLevel(eff, stack);
            if (lvl > 0) speed += lvl * lvl + 1;
        }
        if (stack.isSuitableFor(state)) speed += 0.5;
        return speed;
    }

    private void select(BlockState state) {
        if (mc.player == null || state.isAir()) return;
        if (creativeOff.get() && mc.player.isCreative()) return;
        var inv = mc.player.getInventory();
        int current = inv.getSelectedSlot();
        double best = score(inv.getStack(current), state);
        int bestSlot = current;
        for (int i = 0; i < 9; i++) {
            double s = score(inv.getStack(i), state);
            if (s > best + 1e-6) {
                best = s;
                bestSlot = i;
            }
        }
        if (bestSlot != current) {
            if (previousSlot < 0) previousSlot = current;
            inv.setSelectedSlot(bestSlot);
        }
        idleTicks = 0;
    }
}
