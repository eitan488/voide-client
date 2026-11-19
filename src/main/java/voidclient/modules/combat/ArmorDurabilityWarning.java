package voidclient.modules.combat;

import net.minecraft.item.ItemStack;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;
import voidclient.utilities.Sounds;

/** Warns (sound, toast, screen flash) when worn armour drops below a threshold. */
public class ArmorDurabilityWarning extends Module {
    public final NumberSetting threshold = add(new NumberSetting("Threshold", "Warn below this durability.", 15, 1, 60, 1).unit("%"));
    public final NumberSetting repeat = add(new NumberSetting("Repeat Every", "Seconds between repeated warnings.", 20, 3, 120, 1).unit("s"));
    public final SoundSetting sound = add(new SoundSetting("Sound", "Warning sound.", Sounds.ClientSound.WARNING, true, 0.8));
    public final BooleanSetting flash = add(new BooleanSetting("Screen Flash", "Pulse the screen edges.", true));
    public final ColorSetting flashColor = add(new ColorSetting("Flash Color", "Edge flash colour.", 0x80FF3C00).visibleWhen(flash::get));

    private long lastWarn;
    private boolean low;

    public ArmorDurabilityWarning() {
        super("Armor Durability Warning", "Alerts you before your armour breaks.", Category.COMBAT);
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            String worst = null;
            float min = 1f;
            for (var slot : InventoryUtil.ARMOR) {
                ItemStack s = mc.player.getEquippedStack(slot);
                if (s.isEmpty() || !s.isDamageable()) continue;
                float f = InventoryUtil.durability(s);
                if (f < min) {
                    min = f;
                    worst = s.getName().getString();
                }
            }
            low = worst != null && min * 100 < threshold.get();
            if (low && System.currentTimeMillis() - lastWarn > repeat.get() * 1000) {
                lastWarn = System.currentTimeMillis();
                sound.play();
                Notifications.warning("Armor breaking", worst + " at " + Math.round(min * 100) + "%");
            }
        });
        on(Events.HudRender.class, e -> {
            if (!low || !flash.get()) return;
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0));
            int c = ColorUtil.multiplyAlpha(flashColor.getColor(), pulse);
            float edge = Math.min(e.width, e.height) * 0.08f;
            Render2D.gradientV(e.ctx, 0, 0, e.width, edge, c, 0);
            Render2D.gradientV(e.ctx, 0, e.height - edge, e.width, edge, 0, c);
            Render2D.gradientH(e.ctx, 0, 0, edge, e.height, c, 0);
            Render2D.gradientH(e.ctx, e.width - edge, 0, edge, e.height, 0, c);
        });
    }
}
