package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.EntityOutlines;
import voidclient.render.RenderManager;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Rarity-coloured glow on dropped items with floating names for rare drops. */
public class ItemHighlight extends Module implements EntityOutlines.Provider {
    public final MultiSelectSetting rarities = add(new MultiSelectSetting("Rarities", "Rarities to highlight.", List.of("Common", "Uncommon", "Rare", "Epic"), Set.of("Uncommon", "Rare", "Epic")));
    public final BooleanSetting names = add(new BooleanSetting("Names", "Floating names for highlighted items.", true));
    public final NumberSetting range = add(new NumberSetting("Range", "Highlight range.", 32, 4, 128, 4).unit("m"));

    public ItemHighlight() {
        super("Item Highlight", "Glow dropped items by rarity and label valuable drops.", Category.VISUALS);
        EntityOutlines.register(this);
        on(Events.WorldRender.class, e -> {
            if (!names.get() || mc.world == null) return;
            for (Entity ent : mc.world.getEntities()) {
                if (outlineColor(ent) == 0 || !(ent instanceof ItemEntity it)) continue;
                var s = it.getStack();
                String label = s.getName().getString() + (s.getCount() > 1 ? " x" + s.getCount() : "");
                RenderManager.text(Text.literal(label), ent.getLerpedPos(e.tickDelta).add(0, 0.6, 0), 0.18f, color(s.getRarity()) | 0xFF000000, 0x80000000, false, true);
            }
        });
    }

    private int color(Rarity r) {
        return switch (r) {
            case UNCOMMON -> 0xFFFF55;
            case RARE -> 0x55FFFF;
            case EPIC -> 0xFF55FF;
            default -> 0xFFFFFF;
        };
    }

    @Override
    public boolean outlineActive() { return isEnabled(); }

    @Override
    public int outlineColor(Entity e) {
        if (!(e instanceof ItemEntity it) || mc.player == null || e.squaredDistanceTo(mc.player) > range.get() * range.get()) return 0;
        Rarity r = it.getStack().getRarity();
        String n = r.name().charAt(0) + r.name().substring(1).toLowerCase(Locale.ROOT);
        return rarities.isSelected(n) ? color(r) | 0x01000000 : 0;
    }
}
