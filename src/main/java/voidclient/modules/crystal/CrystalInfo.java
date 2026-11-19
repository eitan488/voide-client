package voidclient.modules.crystal;

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

import java.util.Locale;

/** Compact crystal status: crystals, totems, slot, lowest armour and a READY indicator. */
public class CrystalInfo extends HudModule {
    public final ModeSetting iconStyle = add(new ModeSetting("Icon Style", "Item icons, text labels or minimal.", "Items", "Items", "Text", "Minimal"));
    public final ModeSetting numberFormat = add(new ModeSetting("Number Format", "Plain counts or stacks.", "Plain", "Plain", "Stacks"));
    public final NumberSetting crystalWarn = add(new NumberSetting("Crystal Warning", "Warn at or below this many crystals.", 16, 0, 256, 1));
    public final NumberSetting totemWarn = add(new NumberSetting("Totem Warning", "Warn at or below this many totems.", 1, 0, 20, 1));
    public final NumberSetting armorWarn = add(new NumberSetting("Armor Warning", "Warn below this armour durability.", 20, 1, 100, 1).unit("%"));
    public final BooleanSetting status = add(new BooleanSetting("Status Indicator", "READY / NOT READY badge.", true));

    public CrystalInfo() {
        super("Crystal Information", "Crystal count, totems, slot, armour and readiness.", Category.CRYSTAL, 0.36, 0.005);
    }

    private String fmt(int n) { return InventoryUtil.formatCount(n, numberFormat.is("Stacks")); }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        int crystals = InventoryUtil.count(Items.END_CRYSTAL), totems = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
        float armor = InventoryUtil.lowestArmor();
        int slot = mc.player == null ? 0 : mc.player.getInventory().getSelectedSlot() + 1;
        float health = mc.player == null ? 20 : mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean offTotem = mc.player != null && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        boolean ready = crystals > 0 && InventoryUtil.countHotbar(Items.END_CRYSTAL) > 0 && (offTotem || totems > 0);
        String[][] cells = {{"C", fmt(crystals)}, {"T", fmt(totems)}, {"S", String.valueOf(slot)}, {"A", Math.round(armor * 100) + "%"}, {"H", String.format(Locale.ROOT, "%.0f", health)}};
        int[] colors = {crystals <= crystalWarn.getInt() ? 0xFFFF3C6E : textColor.getColor(), totems <= totemWarn.getInt() ? 0xFFFF3C6E : textColor.getColor(),
                textColor.getColor(), armor * 100 < armorWarn.get() ? 0xFFFF3C6E : ColorUtil.health(armor), ColorUtil.health(health / 20f)};
        ItemStack[] icons = {new ItemStack(Items.END_CRYSTAL), new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.CLOCK), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.GOLDEN_APPLE)};
        String[] names = {"Crystals", "Totems", "Slot", "Armor", "Health"};
        float x = 3;
        float h = 20;
        for (int i = 0; i < cells.length; i++) {
            switch (iconStyle.get()) {
                case "Items" -> {
                    x += 17 + textWidth(cells[i][1]) + 6;
                }
                case "Text" -> x += textWidth(names[i] + " " + cells[i][1]) + 8;
                default -> x += textWidth(cells[i][0] + cells[i][1]) + 6;
            }
        }
        float w = x + (status.get() ? textWidth("NOT READY") + 10 : 0);
        size(w, h);
        panel(ctx, 0, 0, w, h);
        x = 3;
        for (int i = 0; i < cells.length; i++) {
            switch (iconStyle.get()) {
                case "Items" -> {
                    ctx.drawItem(icons[i], (int) x, 2);
                    text(ctx, cells[i][1], x + 17, 6, colors[i]);
                    x += 17 + textWidth(cells[i][1]) + 6;
                }
                case "Text" -> {
                    text(ctx, names[i] + " ", x, 6, dim(textColor.getColor()));
                    text(ctx, cells[i][1], x + textWidth(names[i] + " "), 6, colors[i]);
                    x += textWidth(names[i] + " " + cells[i][1]) + 8;
                }
                default -> {
                    text(ctx, cells[i][0], x, 6, accent());
                    text(ctx, cells[i][1], x + textWidth(cells[i][0]), 6, colors[i]);
                    x += textWidth(cells[i][0] + cells[i][1]) + 6;
                }
            }
        }
        if (status.get()) {
            String s = ready ? "READY" : "NOT READY";
            int c = ready ? 0xFF3CFFA0 : 0xFFFF3C6E;
            Render2D.roundRect(ctx, x, 4, textWidth(s) + 6, 12, 3, ColorUtil.withAlpha(c, 60));
            text(ctx, s, x + 3, 6, c);
        }
    }
}
