package voidclient.modules.player;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.MathUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Active status effects with icons, levels, timers and expiry warnings. */
public class PotionHud extends HudModule {
    public static PotionHud INSTANCE;

    public final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla Effects", "Hide the vanilla top-right effect icons.", true));
    public final ModeSetting sort = add(new ModeSetting("Sort", "Effect order.", "Duration", "Duration", "Name", "Amplifier"));
    public final BooleanSetting icons = add(new BooleanSetting("Icons", "Draw effect icons.", true));
    public final BooleanSetting bar = add(new BooleanSetting("Duration Bar", "Bar under each effect (relative to 3 min).", true));
    public final NumberSetting warn = add(new NumberSetting("Warn Below", "Blink when less than this many seconds remain.", 10, 0, 60, 1).unit("s"));
    public final BooleanSetting typeColors = add(new BooleanSetting("Type Colors", "Green for beneficial, red for harmful effects.", true));

    public PotionHud() {
        super("Potion HUD", "Active potion effects with timers, icons and warnings.", Category.PLAYER, 0.20, 0.075);
        INSTANCE = this;
    }

    public static boolean hidesVanilla() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideVanilla.get(); }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<StatusEffectInstance> effects = new ArrayList<>();
        if (mc.player != null) effects.addAll(mc.player.getStatusEffects());
        if (effects.isEmpty() && editor) {
            effects.add(new StatusEffectInstance(StatusEffects.STRENGTH, 2400, 1));
            effects.add(new StatusEffectInstance(StatusEffects.SPEED, 160, 0));
            effects.add(new StatusEffectInstance(StatusEffects.POISON, 300, 0));
        }
        if (effects.isEmpty()) {
            size(0, 0);
            return;
        }
        switch (sort.get()) {
            case "Name" -> effects.sort(Comparator.comparing(e -> e.getEffectType().value().getName().getString()));
            case "Amplifier" -> effects.sort(Comparator.comparingInt(StatusEffectInstance::getAmplifier).reversed());
            default -> effects.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));
        }
        float row = 20, w = 0;
        List<String> names = new ArrayList<>();
        for (StatusEffectInstance e : effects) {
            String n = e.getEffectType().value().getName().getString() + (e.getAmplifier() > 0 ? " " + Text.translatable("enchantment.level." + (e.getAmplifier() + 1)).getString() : "");
            names.add(n);
            w = Math.max(w, textWidth(n));
        }
        w += icons.get() ? 30 : 10;
        float h = effects.size() * row + 4;
        size(w + 34, h);
        panel(ctx, 0, 0, w + 34, h);
        float y = 2;
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int dur = e.getDuration();
            boolean infinite = e.isInfinite();
            boolean blink = !infinite && dur < warn.get() * 20 && (System.currentTimeMillis() / 300) % 2 == 0;
            float x = 4;
            if (icons.get()) {
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(e.getEffectType()), (int) x, (int) y + 1, 18, 18);
                x += 22;
            }
            int nameColor = typeColors.get() ? (e.getEffectType().value().isBeneficial() ? 0xFF7CFFB0 : 0xFFFF6B7A) : textColor.getColor();
            text(ctx, names.get(i), x, y + 2, nameColor);
            String time = infinite ? "∞" : MathUtil.formatTicks(dur);
            text(ctx, time, x, y + 11, blink ? 0xFFFF3C3C : dim(textColor.getColor()));
            if (bar.get() && !infinite) {
                float frac = Math.min(1f, dur / 3600f);
                Render2D.rect(ctx, x + textWidth(time) + 4, y + 14, 30 * frac, 2, ColorUtil.withAlpha(e.getEffectType().value().getColor(), 255));
            }
            y += row;
        }
    }
}
