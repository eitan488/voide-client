package voidclient.modules.combat;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.utilities.InventoryUtil;
import voidclient.utilities.Sounds;

import java.util.HashMap;
import java.util.Map;

/** Coaching alerts during fights and training: low health, totems, potions, target low. */
public class TrainingNotifications extends Module {
    public final BooleanSetting lowHealth = add(new BooleanSetting("Low Health", "Alert when your health is low.", true));
    public final NumberSetting healthThreshold = add(new NumberSetting("Health Threshold", "Hearts considered low.", 6, 1, 20, 0.5).visibleWhen(lowHealth::get));
    public final BooleanSetting totemPop = add(new BooleanSetting("Totem Pop", "Alert when you or your target pops.", true));
    public final BooleanSetting lowTotems = add(new BooleanSetting("Low Totems", "Alert when you run low on totems.", true));
    public final NumberSetting totemThreshold = add(new NumberSetting("Totem Threshold", "Totems considered low.", 1, 0, 10, 1).visibleWhen(lowTotems::get));
    public final BooleanSetting potionExpiry = add(new BooleanSetting("Potion Expiry", "Alert 10 s before a beneficial effect ends.", true));
    public final BooleanSetting targetLow = add(new BooleanSetting("Target Low", "Alert when your target drops under 5 hearts.", true));
    public final SoundSetting sound = add(new SoundSetting("Sound", "Alert sound.", Sounds.ClientSound.NOTIFICATION, true, 0.5));

    private final Map<String, Long> cooldowns = new HashMap<>();

    public TrainingNotifications() {
        super("Training Notifications", "Coaching alerts for health, totems, potions and targets.", Category.COMBAT);
        alsoListIn(Category.CRYSTAL, Category.ANCHOR);
        on(Events.TickPost.class, e -> tick());
        on(Events.TotemPop.class, e -> {
            if (!totemPop.get()) return;
            if (e.entity() == mc.player) alert("pop-self", "Totem popped", "You popped (" + e.pops() + ")", 0, Notifications.Type.WARNING);
            else if (e.entity() == VoidClient.get().combat().lastTarget()) alert("pop-target", "Target popped", e.entity().getName().getString() + " -" + e.pops(), 0, Notifications.Type.SUCCESS);
        });
    }

    private void alert(String key, String title, String body, long cooldownMs, Notifications.Type type) {
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(key, 0L) < cooldownMs) return;
        cooldowns.put(key, now);
        Notifications.push(title, body, type);
        sound.play();
    }

    private void tick() {
        var p = mc.player;
        if (p == null) return;
        if (lowHealth.get() && p.getHealth() + p.getAbsorptionAmount() <= healthThreshold.get() * 2) {
            alert("health", "Low health", String.format(java.util.Locale.ROOT, "%.1f hearts", (p.getHealth() + p.getAbsorptionAmount()) / 2), 8000, Notifications.Type.ERROR);
        }
        if (lowTotems.get() && InventoryUtil.count(Items.TOTEM_OF_UNDYING) <= totemThreshold.getInt()) {
            alert("totems", "Low totems", InventoryUtil.count(Items.TOTEM_OF_UNDYING) + " left", 30000, Notifications.Type.WARNING);
        }
        if (potionExpiry.get()) {
            for (StatusEffectInstance i : p.getStatusEffects()) {
                if (!i.isInfinite() && i.getEffectType().value().isBeneficial() && i.getDuration() == 200) {
                    alert("pot-" + i.getTranslationKey(), "Effect ending", i.getEffectType().value().getName().getString() + " in 10 s", 1000, Notifications.Type.INFO);
                }
            }
        }
        var t = VoidClient.get().combat().lastTarget();
        if (targetLow.get() && t != null && t.getHealth() + t.getAbsorptionAmount() <= 10 && VoidClient.get().combat().sinceAttackMs() < 5000) {
            alert("target-" + t.getId(), "Target low", t.getName().getString() + " under 5 hearts", 10000, Notifications.Type.SUCCESS);
        }
    }
}
