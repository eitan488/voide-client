package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.render.Render2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Attack cooldown plus every item currently on cooldown in your inventory. */
public class CooldownInformation extends HudModule {
    public final BooleanSetting attack = add(new BooleanSetting("Attack Cooldown", "Show weapon charge.", true));
    public final BooleanSetting items = add(new BooleanSetting("Item Cooldowns", "Pearls, chorus, shields, wind charges...", true));
    public final ColorSetting barColor = add(new ColorSetting("Bar Color", "Cooldown bar colour.", 0xFF00F0FF).themed());

    public CooldownInformation() {
        super("Cooldown Information", "Attack and item cooldowns as bars.", Category.COMBAT, 0.40, 0.62);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<ItemStack> cooling = new ArrayList<>();
        List<Float> progress = new ArrayList<>();
        if (mc.player != null && items.get()) {
            Set<Item> seen = new HashSet<>();
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.isEmpty() || !seen.add(s.getItem())) continue;
                float p = mc.player.getItemCooldownManager().getCooldownProgress(s, delta);
                if (p > 0) {
                    cooling.add(s);
                    progress.add(p);
                }
            }
        }
        float attackP = mc.player == null ? 1 : mc.player.getAttackCooldownProgress(delta);
        boolean showAttack = attack.get() && (attackP < 1 || editor);
        int rows = cooling.size() + (showAttack ? 1 : 0);
        float w = 90, h = rows * 18 + 4;
        size(w, Math.max(h, 8));
        if (rows == 0) return;
        panel(ctx, 0, 0, w, h);
        float y = 2;
        if (showAttack) {
            text(ctx, "Attack", 4, y + 4, textColor.getColor());
            Render2D.roundRect(ctx, 40, y + 7, 46, 3, 1.5f, 0x50000000);
            Render2D.roundRect(ctx, 40, y + 7, 46 * attackP, 3, 1.5f, barColor.getColor());
            y += 18;
        }
        for (int i = 0; i < cooling.size(); i++) {
            ctx.drawItem(cooling.get(i), 2, (int) y);
            Render2D.roundRect(ctx, 22, y + 7, 64, 3, 1.5f, 0x50000000);
            Render2D.roundRect(ctx, 22, y + 7, 64 * (1 - progress.get(i)), 3, 1.5f, barColor.getColor());
            y += 18;
        }
    }
}
