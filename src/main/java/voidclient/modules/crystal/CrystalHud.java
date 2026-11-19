package voidclient.modules.crystal;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.animations.Animation;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

import java.util.Locale;

/** All-in-one Crystal PvP panel: health, armour, offhand, crystals, totems, effects, info. */
public class CrystalHud extends HudModule {
    public final BooleanSetting showArmor = add(new BooleanSetting("Armor", "Armour pieces with durability.", true));
    public final BooleanSetting showEffects = add(new BooleanSetting("Potion Effects", "Active effect icons.", true));
    public final BooleanSetting showCoords = add(new BooleanSetting("Coordinates", "Your coordinates.", true));
    public final BooleanSetting showPerf = add(new BooleanSetting("FPS / Ping", "Frame rate and latency.", true));
    public final BooleanSetting showStats = add(new BooleanSetting("Combat Statistics", "Pops and attacks this session.", true));
    public final NumberSetting animation = add(new NumberSetting("Animation", "Health bar animation length.", 250, 0, 1000, 25).unit("ms"));

    private final Animation hp = new Animation(1, 250);

    public CrystalHud() {
        super("Crystal PvP HUD", "Everything you need for crystal PvP in one panel.", Category.CRYSTAL, 0.005, 0.455);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float w = 150;
        var p = mc.player;
        boolean anyArmor = false;
        if (p != null) for (var slot : InventoryUtil.ARMOR) anyArmor |= !p.getEquippedStack(slot).isEmpty();
        // Rows with nothing to show collapse outside the editor so the panel stays compact.
        boolean armorRow = showArmor.get() && (anyArmor || editor);
        boolean effectRow = showEffects.get() && (editor || (p != null && !p.getStatusEffects().isEmpty()));
        int rows = 4 + (armorRow ? 1 : 0) + (effectRow ? 1 : 0) + (showCoords.get() && p != null ? 1 : 0) + (showPerf.get() ? 1 : 0) + (showStats.get() ? 1 : 0);
        float h = rows * 13 + 18;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        text(ctx, "CRYSTAL PVP", 5, 4, accent());
        float y = 16;
        float health = p == null ? 20 : p.getHealth(), abs = p == null ? 0 : p.getAbsorptionAmount(), max = p == null ? 20 : Math.max(1, p.getMaxHealth());
        hp.target(health / max);
        float f = animation.get() <= 0 ? health / max : Math.max(0, Math.min(1, hp.get()));
        text(ctx, String.format(Locale.ROOT, "HP %.1f", health + abs), 5, y, ColorUtil.health(health / max));
        Render2D.roundRect(ctx, 52, y + 2, w - 57, 5, 2.5f, 0x60000000);
        Render2D.roundRectH(ctx, 52, y + 2, Math.max(5, (w - 57) * f), 5, 2.5f, accent(), ColorUtil.health(f));
        y += 13;
        int crystals = InventoryUtil.count(Items.END_CRYSTAL), totems = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
        item(ctx, new ItemStack(Items.END_CRYSTAL), 4, y - 3);
        text(ctx, String.valueOf(crystals), 22, y + 1, crystals == 0 ? 0xFFFF3C6E : textColor.getColor());
        item(ctx, new ItemStack(Items.TOTEM_OF_UNDYING), 52, y - 3);
        text(ctx, String.valueOf(totems), 70, y + 1, totems <= 1 ? 0xFFFF3C6E : textColor.getColor());
        item(ctx, new ItemStack(Items.OBSIDIAN), 100, y - 3);
        text(ctx, String.valueOf(InventoryUtil.count(Items.OBSIDIAN)), 118, y + 1, textColor.getColor());
        y += 13;
        ItemStack off = p == null ? ItemStack.EMPTY : p.getOffHandStack();
        text(ctx, "Offhand", 5, y, dim(textColor.getColor()));
        text(ctx, off.isEmpty() ? "empty" : off.getName().getString(), 50, y, off.isOf(Items.TOTEM_OF_UNDYING) ? 0xFF3CFFA0 : off.isEmpty() ? 0xFFFF3C6E : textColor.getColor());
        y += 13;
        text(ctx, "Slot", 5, y, dim(textColor.getColor()));
        text(ctx, p == null ? "-" : String.valueOf(p.getInventory().getSelectedSlot() + 1), 50, y, textColor.getColor());
        y += 13;
        if (armorRow) {
            for (int i = 0; i < 4; i++) {
                ItemStack s = p == null ? ItemStack.EMPTY : p.getEquippedStack(InventoryUtil.ARMOR[i]);
                float x = 4 + i * 36;
                if (!s.isEmpty()) {
                    item(ctx, s, x, y - 3);
                    text(ctx, Math.round(InventoryUtil.durability(s) * 100) + "%", x + 17, y + 1, ColorUtil.health(InventoryUtil.durability(s)));
                }
            }
            y += 13;
        }
        if (effectRow) {
            float x = 5;
            if (p != null) for (StatusEffectInstance e : p.getStatusEffects()) {
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(e.getEffectType()), (int) x, (int) y - 1, 11, 11);
                x += 13;
                if (x > w - 14) break;
            }
            y += 13;
        }
        if (showCoords.get() && p != null) {
            text(ctx, String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()), 5, y, dim(textColor.getColor()));
            y += 13;
        }
        if (showPerf.get()) {
            int ping = -1;
            if (p != null && mc.getNetworkHandler() != null) {
                var en = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (en != null) ping = en.getLatency();
            }
            text(ctx, mc.getCurrentFps() + " fps  " + (ping < 0 ? "local" : ping + " ms"), 5, y, dim(textColor.getColor()));
            y += 13;
        }
        if (showStats.get()) {
            var s = VoidClient.get().session();
            text(ctx, "Pops " + s.totemPops() + "  Attacks " + s.attacks(), 5, y, dim(textColor.getColor()));
        }
    }

    private void item(DrawContext ctx, ItemStack s, float x, float y) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(0.85f, 0.85f);
        ctx.drawItem(s, 0, 0);
        m.popMatrix();
    }
}
