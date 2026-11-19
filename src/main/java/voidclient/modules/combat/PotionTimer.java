package voidclient.modules.combat;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;
import voidclient.utilities.MathUtil;
import voidclient.utilities.Sounds;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Big countdowns for the combat potions you choose, with an expiry sound. */
public class PotionTimer extends HudModule {
    private static final Map<String, RegistryEntry<StatusEffect>> EFFECTS = new LinkedHashMap<>();

    static {
        EFFECTS.put("Strength", StatusEffects.STRENGTH);
        EFFECTS.put("Speed", StatusEffects.SPEED);
        EFFECTS.put("Resistance", StatusEffects.RESISTANCE);
        EFFECTS.put("Fire Resistance", StatusEffects.FIRE_RESISTANCE);
        EFFECTS.put("Regeneration", StatusEffects.REGENERATION);
        EFFECTS.put("Absorption", StatusEffects.ABSORPTION);
        EFFECTS.put("Slow Falling", StatusEffects.SLOW_FALLING);
        EFFECTS.put("Invisibility", StatusEffects.INVISIBILITY);
        EFFECTS.put("Jump Boost", StatusEffects.JUMP_BOOST);
    }

    public final MultiSelectSetting tracked = add(new MultiSelectSetting("Tracked Effects", "Effects to time.", List.copyOf(EFFECTS.keySet()),
            Set.of("Strength", "Speed", "Resistance", "Fire Resistance")));
    public final NumberSetting warn = add(new NumberSetting("Warn At", "Warn when this many seconds remain.", 15, 0, 120, 1).unit("s"));
    public final SoundSetting sound = add(new SoundSetting("Expiry Sound", "Played at the warning time.", Sounds.ClientSound.WARNING, true, 0.7));

    private final Set<String> warned = new HashSet<>();

    public PotionTimer() {
        super("Potion Timer", "Countdowns for key combat potions.", Category.COMBAT, 0.86, 0.30);
        on(Events.TickPost.class, e -> {
            if (mc.player == null) return;
            for (var en : EFFECTS.entrySet()) {
                StatusEffectInstance i = mc.player.getStatusEffect(en.getValue());
                if (i == null || i.isInfinite()) {
                    warned.remove(en.getKey());
                    continue;
                }
                if (tracked.isSelected(en.getKey()) && i.getDuration() <= warn.get() * 20 && warned.add(en.getKey())) sound.play();
                if (i.getDuration() > warn.get() * 20) warned.remove(en.getKey());
            }
        });
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        float y = 2, w = 74;
        int rows = 0;
        for (var en : EFFECTS.entrySet()) {
            if (!tracked.isSelected(en.getKey())) continue;
            StatusEffectInstance i = mc.player == null ? null : mc.player.getStatusEffect(en.getValue());
            if (i == null && !(editor && rows < 2)) continue;
            rows++;
        }
        size(w, Math.max(1, rows) * 22 + 2);
        if (rows == 0) return;
        panel(ctx, 0, 0, w, rows * 22 + 2);
        int shown = 0;
        for (var en : EFFECTS.entrySet()) {
            if (!tracked.isSelected(en.getKey())) continue;
            StatusEffectInstance i = mc.player == null ? null : mc.player.getStatusEffect(en.getValue());
            if (i == null) {
                if (!(editor && shown < 2)) continue;
                i = new StatusEffectInstance(en.getValue(), 1800, 1);
            }
            shown++;
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(en.getValue()), 3, (int) y + 1, 18, 18);
            int dur = i.getDuration();
            boolean low = dur <= warn.get() * 20;
            text(ctx, i.isInfinite() ? "∞" : MathUtil.formatTicks(dur), 25, y + 3, low ? 0xFFFF3C6E : textColor.getColor());
            Render2D.rect(ctx, 25, y + 14, 45 * Math.min(1f, dur / 3600f), 2, low ? 0xFFFF3C6E : accent());
            y += 22;
        }
    }
}
