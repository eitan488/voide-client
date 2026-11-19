package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import voidclient.core.Category;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.StringSetting;
import voidclient.utilities.InventoryUtil;

/** Dropped items with name/count labels; optional item filter and rarity colours. */
public class ItemEsp extends EntityEspBase {
    public final BooleanSetting rarityColor = add(new BooleanSetting("Rarity Color", "Colour by item rarity.", true));
    public final StringSetting filter = add(new StringSetting("Only Items", "Comma-separated item ids (empty = all items).", "", 400));

    public ItemEsp() {
        super("Item ESP", "Highlights dropped items with labels.");
        alsoListIn(Category.VISUALS);
        color.defaultTo(new voidclient.core.setting.ColorSetting.ColorValue(0xFFFFD23C, false, false));
    }

    @Override
    protected boolean accepts(Entity e) {
        if (!(e instanceof ItemEntity it)) return false;
        if (filter.get().isBlank()) return true;
        return InventoryUtil.items(filter.get()).contains(it.getStack().getItem());
    }

    @Override
    protected int colorFor(Entity e) {
        if (rarityColor.get() && e instanceof ItemEntity it) {
            return switch (it.getStack().getRarity()) {
                case UNCOMMON -> 0xFFFFFF55;
                case RARE -> 0xFF55FFFF;
                case EPIC -> 0xFFFF55FF;
                default -> color.getColor();
            };
        }
        return color.getColor();
    }

    @Override
    protected String label(Entity e) {
        var s = ((ItemEntity) e).getStack();
        return s.getName().getString() + (s.getCount() > 1 ? " x" + s.getCount() : "");
    }
}
