package voidclient.modules.player;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.StringSetting;
import voidclient.events.Events;
import voidclient.utilities.InventoryUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory housekeeping while your inventory screen is open: merges partial stacks, throws
 * configured junk and sorts the main inventory, one click step every few ticks.
 */
public class InventoryManager extends Module {
    public final BooleanSetting stack = add(new BooleanSetting("Merge Stacks", "Combine partial stacks of the same item.", true));
    public final BooleanSetting dropJunk = add(new BooleanSetting("Drop Junk", "Throw out items from the junk list.", false));
    public final StringSetting junk = add(new StringSetting("Junk Items", "Comma-separated item ids to throw.", "rotten_flesh,poisonous_potato,dead_bush", 400).visibleWhen(dropJunk::get));
    public final BooleanSetting sort = add(new BooleanSetting("Sort", "Sort the main inventory (hotbar untouched).", true));
    public final ModeSetting sortMode = add(new ModeSetting("Sort Mode", "Sort order.", "Category", "Category", "Name", "Count").visibleWhen(sort::get));
    public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between click steps.", 2, 0, 10, 1));
    public final BooleanSetting onlyInventory = add(new BooleanSetting("Only In Inventory", "Run only while the player inventory screen is open.", true));

    private int cooldown;
    private final List<int[]> queue = new ArrayList<>();

    public InventoryManager() {
        super("Inventory Manager", "Merges stacks, drops junk and sorts your inventory.", Category.PLAYER);
        on(Events.TickPost.class, e -> tick());
    }

    @Override
    protected void onEnable() { queue.clear(); }

    private void click(int slot, int button, SlotActionType type) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, button, type, mc.player);
    }

    private void tick() {
        if (!inWorld() || mc.interactionManager == null) return;
        if (onlyInventory.get() && !(mc.currentScreen instanceof InventoryScreen)) {
            queue.clear();
            return;
        }
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) return;
        if (cooldown-- > 0) return;
        cooldown = delay.getInt();
        if (!queue.isEmpty()) {
            int[] step = queue.remove(0);
            click(step[0], step[1], SlotActionType.values()[step[2]]);
            return;
        }
        if (dropJunk.get() && planDrop()) return;
        if (stack.get() && planMerge()) return;
        if (sort.get()) planSort();
    }

    /** Main inventory = screen slots 9..35 (inventory indices 9..35). */
    private ItemStack at(int slot) { return mc.player.getInventory().getStack(slot); }

    private boolean planDrop() {
        List<Item> junkItems = InventoryUtil.items(junk.get());
        for (int s = 9; s <= 35; s++) {
            if (!at(s).isEmpty() && junkItems.contains(at(s).getItem())) {
                queue.add(new int[]{s, 1, SlotActionType.THROW.ordinal()});
                return true;
            }
        }
        return false;
    }

    private boolean planMerge() {
        for (int a = 9; a <= 35; a++) {
            ItemStack sa = at(a);
            if (sa.isEmpty() || !sa.isStackable() || sa.getCount() >= sa.getMaxCount()) continue;
            for (int b = a + 1; b <= 35; b++) {
                ItemStack sb = at(b);
                if (!sb.isEmpty() && ItemStack.areItemsAndComponentsEqual(sa, sb) && sb.getCount() < sb.getMaxCount()) {
                    queue.add(new int[]{b, 0, SlotActionType.PICKUP.ordinal()});
                    queue.add(new int[]{a, 0, SlotActionType.PICKUP.ordinal()});
                    queue.add(new int[]{b, 0, SlotActionType.PICKUP.ordinal()});
                    return true;
                }
            }
        }
        return false;
    }

    private Comparator<ItemStack> comparator() {
        Comparator<ItemStack> empty = Comparator.comparing(ItemStack::isEmpty);
        return switch (sortMode.get()) {
            case "Name" -> empty.thenComparing(s -> s.getName().getString());
            case "Count" -> empty.thenComparing(ItemStack::getCount, Comparator.reverseOrder()).thenComparing(s -> Registries.ITEM.getId(s.getItem()).toString());
            default -> empty.thenComparing(s -> Registries.ITEM.getRawId(s.getItem())).thenComparing(ItemStack::getCount, Comparator.reverseOrder());
        };
    }

    private void planSort() {
        List<ItemStack> current = new ArrayList<>();
        for (int s = 9; s <= 35; s++) current.add(at(s));
        Comparator<ItemStack> cmp = comparator();
        for (int i = 0; i < current.size(); i++) {
            int best = i;
            for (int j = i + 1; j < current.size(); j++) if (cmp.compare(current.get(j), current.get(best)) < 0) best = j;
            if (best != i && cmp.compare(current.get(best), current.get(i)) < 0) {
                int a = 9 + i, b = 9 + best;
                queue.add(new int[]{b, 0, SlotActionType.PICKUP.ordinal()});
                queue.add(new int[]{a, 0, SlotActionType.PICKUP.ordinal()});
                if (!current.get(i).isEmpty()) queue.add(new int[]{b, 0, SlotActionType.PICKUP.ordinal()});
                return;
            }
        }
    }
}
