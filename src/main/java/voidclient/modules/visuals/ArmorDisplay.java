package voidclient.modules.visuals;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

/** Other players' armour durability (per piece) and held item, above their heads. */
public class ArmorDisplay extends Module {
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Display range.", 32, 4, 128, 4).unit("m"));
    public final BooleanSetting heldItem = add(new BooleanSetting("Held Item", "Show the held item name.", true));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Text size.", 0.8, 0.3, 2, 0.05));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw through blocks.", true));

    public ArmorDisplay() {
        super("Armor Display", "Armour durability of other players above their heads.", Category.VISUALS);
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null) return;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.squaredDistanceTo(mc.player) > maxDistance.get() * maxDistance.get()) continue;
                MutableText t = Text.empty();
                String[] names = {"H", "C", "L", "B"};
                boolean any = false;
                for (int i = 0; i < 4; i++) {
                    ItemStack s = p.getEquippedStack(InventoryUtil.ARMOR[i]);
                    if (s.isEmpty()) continue;
                    any = true;
                    float f = InventoryUtil.durability(s);
                    int c = ColorUtil.health(f) & 0xFFFFFF;
                    t.append(Text.literal(names[i] + (s.isDamageable() ? Math.round(f * 100) + "% " : " ")).styled(st -> st.withColor(c)));
                }
                if (heldItem.get() && !p.getMainHandStack().isEmpty()) {
                    t.append(Text.literal("| " + p.getMainHandStack().getName().getString()).styled(st -> st.withColor(0xCCCCFF)));
                    any = true;
                }
                if (!any) continue;
                RenderManager.text(t, p.getLerpedPos(e.tickDelta).add(0, p.getHeight() + 0.85, 0), scale.getFloat() * 0.2f, 0xFFFFFFFF, 0x80000000, throughWalls.get(), true);
            }
        });
    }
}
