package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

/** A second, restylable hotbar (e.g. vertical next to the crosshair). */
public class HotbarDisplay extends HudModule {
    public final ModeSetting orientation = add(new ModeSetting("Orientation", "Row or column.", "Vertical", "Horizontal", "Vertical"));
    public final NumberSetting slotSize = add(new NumberSetting("Slot Size", "Slot size.", 18, 16, 26, 1));
    public final ColorSetting highlight = add(new ColorSetting("Selected Color", "Selected slot highlight.", 0xCCB026FF).themed());
    public final BooleanSetting durabilityBars = add(new BooleanSetting("Durability Bars", "Draw durability under tools.", true));
    public final BooleanSetting slotNumbers = add(new BooleanSetting("Slot Numbers", "Tiny 1-9 labels.", false));

    public HotbarDisplay() {
        super("Hotbar Display", "Customisable copy of your hotbar.", Category.COMBAT, 0.30, 0.30);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float s = slotSize.getFloat();
        boolean vertical = orientation.is("Vertical");
        float w = vertical ? s + 4 : 9 * s + 4, h = vertical ? 9 * s + 4 : s + 4;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        int sel = mc.player == null ? 0 : mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            float x = vertical ? 2 : 2 + i * s, y = vertical ? 2 + i * s : 2;
            if (i == sel) {
                Render2D.glow(ctx, x, y, s, s, 3, 0.6f, highlight.getColor());
                Render2D.roundRect(ctx, x, y, s, s, 3, ColorUtil.withAlpha(highlight.getColor(), 90));
            }
            if (mc.player == null) continue;
            ItemStack st = mc.player.getInventory().getStack(i);
            float off = (s - 16) / 2f;
            if (!st.isEmpty()) {
                ctx.drawItem(st, (int) (x + off), (int) (y + off));
                ctx.drawStackOverlay(mc.textRenderer, st, (int) (x + off), (int) (y + off));
                if (durabilityBars.get() && st.isDamageable() && st.getDamage() > 0) {
                    float f = InventoryUtil.durability(st);
                    Render2D.rect(ctx, x + 2, y + s - 2, (s - 4) * f, 1, ColorUtil.health(f));
                }
            }
            if (slotNumbers.get()) text(ctx, String.valueOf(i + 1), x + 1, y + 1, dim(textColor.getColor()));
        }
    }
}
