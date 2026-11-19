package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;
import voidclient.utilities.InventoryUtil;
import voidclient.utilities.Sounds;

/** Totem count with low-stock warning and pop tracking. */
public class TotemCounter extends HudModule {
    public final NumberSetting warnBelow = add(new NumberSetting("Warn Below", "Warning colour at or below this count.", 2, 0, 20, 1));
    public final ColorSetting warnColor = add(new ColorSetting("Warning Color", "Colour when low.", 0xFFFF3C6E));
    public final BooleanSetting flash = add(new BooleanSetting("Flash When Low", "Blink the counter when low.", true));
    public final BooleanSetting showPops = add(new BooleanSetting("Show Pops", "Your totem pops this session.", true));
    public final SoundSetting popSound = add(new SoundSetting("Pop Sound", "Played when you pop a totem.", Sounds.ClientSound.WARNING, true, 0.8));
    public final BooleanSetting offhandMarker = add(new BooleanSetting("Offhand Marker", "Dot when a totem is in your offhand.", true));

    public TotemCounter() {
        super("Totem Counter", "How many Totems of Undying you carry.", Category.COMBAT, 0.40, 0.76);
        alsoListIn(Category.CRYSTAL, Category.ANCHOR);
        on(Events.TotemPop.class, e -> {
            if (e.entity() == mc.player) popSound.play();
        });
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        int n = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
        String s = String.valueOf(n);
        String pops = showPops.get() ? "  -" + VoidClient.get().session().totemPops() : "";
        float w = 22 + textWidth(s + pops) + 4, h = 20;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), 2, 2);
        boolean low = n <= warnBelow.getInt();
        boolean visible = !(low && flash.get()) || (System.currentTimeMillis() / 400) % 2 == 0;
        if (visible) text(ctx, s, 20, 6, low ? warnColor.getColor() : textColor.getColor());
        if (!pops.isEmpty()) text(ctx, pops, 20 + textWidth(s), 6, dim(textColor.getColor()));
        if (offhandMarker.get() && mc.player != null && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            Render2D.circle(ctx, w - 3, 3, 1.6f, 0xFF3CFFA0);
        }
    }
}
