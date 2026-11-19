package voidclient.modules.player;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.component.DataComponentTypes;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

import java.util.List;
import java.util.Set;

/** Refills hotbar stacks from the main inventory when they run low. */
public class AutoRefill extends Module {
    public final NumberSetting threshold = add(new NumberSetting("Threshold", "Refill when a stack has this many items or fewer.", 8, 0, 63, 1));
    public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between refills.", 3, 0, 20, 1));
    public final BooleanSetting onlySelected = add(new BooleanSetting("Only Selected Slot", "Refill only the slot you are holding.", false));
    public final MultiSelectSetting types = add(new MultiSelectSetting("Item Types", "What may be refilled.",
            List.of("Blocks", "Food", "Totems", "Crystals", "Projectiles", "Other"), Set.of("Blocks", "Food", "Totems", "Crystals", "Projectiles", "Other")));
    public final BooleanSetting pauseInScreens = add(new BooleanSetting("Pause In Screens", "Do nothing while any screen is open.", true));

    private int cooldown;

    public AutoRefill() {
        super("Auto Refill", "Keeps hotbar stacks topped up from your inventory.", Category.PLAYER);
        on(Events.TickPost.class, e -> tick());
    }

    private String type(ItemStack s) {
        if (s.isOf(Items.TOTEM_OF_UNDYING)) return "Totems";
        if (s.isOf(Items.END_CRYSTAL)) return "Crystals";
        if (s.getItem() instanceof BlockItem) return "Blocks";
        if (s.contains(DataComponentTypes.FOOD)) return "Food";
        if (s.isOf(Items.ARROW) || s.isOf(Items.ENDER_PEARL) || s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG) || s.isOf(Items.WIND_CHARGE)) return "Projectiles";
        return "Other";
    }

    private final ItemStack[] lastHotbar = new ItemStack[9];

    private void tick() {
        if (!inWorld() || mc.interactionManager == null) return;
        if (pauseInScreens.get() && mc.currentScreen != null) return;
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) return;
        var inv = mc.player.getInventory();
        if (cooldown-- > 0) {
            snapshot();
            return;
        }
        for (int h = 0; h < 9; h++) {
            if (onlySelected.get() && h != inv.getSelectedSlot()) continue;
            ItemStack cur = inv.getStack(h);
            ItemStack ref = cur.isEmpty() ? lastHotbar[h] : cur;
            if (ref == null || ref.isEmpty() || !ref.isStackable()) continue;
            if (!types.isSelected(type(ref))) continue;
            if (!cur.isEmpty() && cur.getCount() > threshold.getInt()) continue;
            for (int s = 9; s <= 35; s++) {
                ItemStack src = inv.getStack(s);
                if (src.isEmpty() || !ItemStack.areItemsAndComponentsEqual(src, ref)) continue;
                int sync = mc.player.playerScreenHandler.syncId;
                if (cur.isEmpty()) {
                    mc.interactionManager.clickSlot(sync, s, h, SlotActionType.SWAP, mc.player);
                } else {
                    mc.interactionManager.clickSlot(sync, s, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(sync, 36 + h, 0, SlotActionType.PICKUP, mc.player);
                    if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) mc.interactionManager.clickSlot(sync, s, 0, SlotActionType.PICKUP, mc.player);
                }
                cooldown = delay.getInt();
                snapshot();
                return;
            }
        }
        snapshot();
    }

    private void snapshot() {
        var inv = mc.player.getInventory();
        for (int h = 0; h < 9; h++) {
            ItemStack s = inv.getStack(h);
            if (!s.isEmpty()) lastHotbar[h] = s.copy();
        }
    }
}
