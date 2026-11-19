package voidclient.modules.player;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

/** Shows your main inventory (and optionally the hotbar) on the HUD. */
public class InventoryHud extends HudModule {
    public final BooleanSetting includeHotbar = add(new BooleanSetting("Include Hotbar", "Also show the hotbar row.", false));
    public final BooleanSetting slotBackground = add(new BooleanSetting("Slot Background", "Draw individual slot backgrounds.", true));
    public final BooleanSetting counts = add(new BooleanSetting("Counts", "Draw stack counts and durability bars.", true));
    public final NumberSetting spacing = add(new NumberSetting("Spacing", "Gap between slots.", 1, 0, 4, 0.5));

    public InventoryHud() {
        super("Inventory HUD", "Your inventory contents on screen.", Category.PLAYER, 0.36, 0.78);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        int rows = includeHotbar.get() ? 4 : 3;
        float sp = spacing.getFloat(), cell = 16 + sp;
        float w = 9 * cell + 4 - sp, h = rows * cell + 4 - sp;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = r < 3 ? 9 + r * 9 + c : c;
                ItemStack s = mc.player != null ? mc.player.getInventory().getStack(slot) : editor && (r + c) % 5 == 0 ? new ItemStack(Items.DIAMOND, 12) : ItemStack.EMPTY;
                float x = 2 + c * cell, y = 2 + r * cell;
                if (slotBackground.get()) Render2D.roundRect(ctx, x, y, 16, 16, 2, ColorUtil.withAlpha(0xFF000000, 70));
                if (s.isEmpty()) continue;
                ctx.drawItem(s, (int) x, (int) y);
                if (counts.get()) ctx.drawStackOverlay(mc.textRenderer, s, (int) x, (int) y);
            }
        }
    }
}
