package voidclient.modules.anchor;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

import java.util.Locale;

/** Anchor PvP panel: looked-at anchor charge, glowstone, anchors, totems, armour, health. */
public class AnchorHud extends HudModule {
    public final BooleanSetting showArmor = add(new BooleanSetting("Armor", "Lowest armour durability.", true));
    public final BooleanSetting showCoords = add(new BooleanSetting("Coordinates", "Your coordinates.", true));
    public final BooleanSetting showStats = add(new BooleanSetting("Combat Statistics", "Session pops and attacks.", true));
    public final BooleanSetting dimensionWarning = add(new BooleanSetting("Dimension Warning", "Warn that anchors don't explode in the Nether.", true));

    public AnchorHud() {
        super("Anchor HUD", "Respawn anchor, glowstone and survival info for anchor PvP.", Category.ANCHOR, 0.005, 0.35);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float w = 150, y = 16;
        var p = mc.player;
        int rows = 5 + (showArmor.get() ? 1 : 0) + (showCoords.get() ? 1 : 0) + (showStats.get() ? 1 : 0);
        boolean nether = mc.world != null && mc.world.getRegistryKey() == net.minecraft.world.World.NETHER;
        if (dimensionWarning.get() && nether) rows++;
        float h = rows * 13 + 18;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        text(ctx, "ANCHOR PVP", 5, 4, accent());
        String anchorInfo = "none in view";
        int charge = -1;
        if (mc.world != null && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.world.getBlockState(hit.getBlockPos());
            if (st.isOf(Blocks.RESPAWN_ANCHOR)) {
                charge = st.get(RespawnAnchorBlock.CHARGES);
                anchorInfo = charge + " / 4 charges";
            }
        }
        text(ctx, "Anchor", 5, y, dim(textColor.getColor()));
        text(ctx, anchorInfo, 52, y, charge < 0 ? dim(textColor.getColor()) : charge == 0 ? 0xFFFFC93C : 0xFFFF3C6E);
        if (charge >= 0) for (int i = 0; i < 4; i++) Render2D.rect(ctx, w - 30 + i * 6, y + 2, 4, 5, i < charge ? 0xFFFFC93C : 0x40FFFFFF);
        y += 13;
        row(ctx, new ItemStack(Items.RESPAWN_ANCHOR), InventoryUtil.count(Items.RESPAWN_ANCHOR), 4, y);
        row(ctx, new ItemStack(Items.GLOWSTONE), InventoryUtil.count(Items.GLOWSTONE), 52, y);
        row(ctx, new ItemStack(Items.TOTEM_OF_UNDYING), InventoryUtil.count(Items.TOTEM_OF_UNDYING), 100, y);
        y += 13;
        text(ctx, "Slot", 5, y, dim(textColor.getColor()));
        text(ctx, p == null ? "-" : String.valueOf(p.getInventory().getSelectedSlot() + 1), 52, y, textColor.getColor());
        y += 13;
        float hp = p == null ? 20 : p.getHealth() + p.getAbsorptionAmount(), max = p == null ? 20 : Math.max(1, p.getMaxHealth());
        text(ctx, String.format(Locale.ROOT, "HP %.1f", hp), 5, y, ColorUtil.health(Math.min(1, hp / max)));
        Render2D.roundRect(ctx, 52, y + 2, w - 57, 5, 2.5f, 0x60000000);
        Render2D.roundRectH(ctx, 52, y + 2, Math.max(5, (w - 57) * Math.min(1, hp / max)), 5, 2.5f, accent(), ColorUtil.health(Math.min(1, hp / max)));
        y += 13;
        if (showArmor.get()) {
            float a = InventoryUtil.lowestArmor();
            text(ctx, "Armor", 5, y, dim(textColor.getColor()));
            text(ctx, Math.round(a * 100) + "% lowest", 52, y, ColorUtil.health(a));
            y += 13;
        }
        if (showCoords.get() && p != null) {
            text(ctx, String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()), 5, y, dim(textColor.getColor()));
            y += 13;
        }
        if (showStats.get()) {
            var s = VoidClient.get().session();
            text(ctx, "Pops " + s.totemPops() + "  Attacks " + s.attacks(), 5, y, dim(textColor.getColor()));
            y += 13;
        }
        if (dimensionWarning.get() && nether) text(ctx, "Nether: anchors won't explode", 5, y, 0xFFFFC93C);
    }

    private void row(DrawContext ctx, ItemStack icon, int count, float x, float y) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y - 3);
        m.scale(0.85f, 0.85f);
        ctx.drawItem(icon, 0, 0);
        m.popMatrix();
        text(ctx, String.valueOf(count), x + 18, y + 1, count == 0 ? 0xFFFF3C6E : textColor.getColor());
    }
}
