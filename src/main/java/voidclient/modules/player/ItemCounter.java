package voidclient.modules.player;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.StringSetting;
import voidclient.utilities.InventoryUtil;

import java.util.List;

/** Counts of chosen items across your whole inventory. */
public class ItemCounter extends HudModule {
    public final StringSetting items = add(new StringSetting("Items", "Comma-separated item ids to count.",
            "end_crystal,totem_of_undying,golden_apple,obsidian,ender_pearl,experience_bottle,glowstone,respawn_anchor", 600));
    public final BooleanSetting hideZero = add(new BooleanSetting("Hide Zero", "Hide items you don't have.", false));
    public final ModeSetting layout = add(new ModeSetting("Layout", "Vertical list or horizontal row.", "Vertical", "Vertical", "Horizontal"));
    public final BooleanSetting stacks = add(new BooleanSetting("Stack Format", "Show 3s+12 instead of 204.", false));

    private List<Item> cache;
    private String cacheKey = "";

    public ItemCounter() {
        super("Item Counter", "Live counts of the items you care about.", Category.PLAYER, 0.005, 0.745);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        if (!items.get().equals(cacheKey)) {
            cache = InventoryUtil.items(items.get());
            cacheKey = items.get();
        }
        boolean vertical = layout.is("Vertical");
        float x = 3, y = 3, maxW = 0, cellW = 0;
        int shown = 0;
        for (Item it : cache) {
            int n = InventoryUtil.count(it);
            if (n == 0 && hideZero.get() && !editor) continue;
            String s = InventoryUtil.formatCount(n, stacks.get());
            cellW = 18 + textWidth(s) + 4;
            maxW = Math.max(maxW, cellW);
            shown++;
        }
        if (shown == 0) {
            size(0, 0);
            return;
        }
        float w = vertical ? maxW + 6 : 0, h = vertical ? shown * 17 + 4 : 21;
        if (!vertical) for (Item it : cache) {
            int n = InventoryUtil.count(it);
            if (n == 0 && hideZero.get() && !editor) continue;
            w += 18 + textWidth(InventoryUtil.formatCount(n, stacks.get())) + 6;
        }
        size(w + 2, h);
        panel(ctx, 0, 0, w + 2, h);
        for (Item it : cache) {
            int n = InventoryUtil.count(it);
            if (n == 0 && hideZero.get() && !editor) continue;
            String s = InventoryUtil.formatCount(n, stacks.get());
            ctx.drawItem(new ItemStack(it), (int) x, (int) y);
            text(ctx, s, x + 18, y + 5, n == 0 ? 0xFFFF5566 : textColor.getColor());
            if (vertical) y += 17;
            else x += 18 + textWidth(s) + 6;
        }
    }
}
