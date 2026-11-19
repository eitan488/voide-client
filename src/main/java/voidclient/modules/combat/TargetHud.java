package voidclient.modules.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.animations.Animation;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.InventoryUtil;

import java.util.Locale;

/** Animated panel for your current target: head, health, armour, distance, pops. */
public class TargetHud extends HudModule {
    public final ModeSetting source = add(new ModeSetting("Target Source", "Which entity is shown.", "Both", "Attacked", "Looking", "Both"));
    public final NumberSetting timeout = add(new NumberSetting("Timeout", "Hide after this long without combat.", 5, 1, 30, 1).unit("s"));
    public final ModeSetting barStyle = add(new ModeSetting("Health Bar", "Health bar style.", "Gradient", "Gradient", "Solid", "Segmented"));
    public final BooleanSetting showArmor = add(new BooleanSetting("Armor", "Show the target's armour.", true));
    public final BooleanSetting showDistance = add(new BooleanSetting("Distance", "Show distance to the target.", true));
    public final BooleanSetting showPops = add(new BooleanSetting("Totem Pops", "Show the target's totem pops.", true));
    public final BooleanSetting winIndicator = add(new BooleanSetting("Win Indicator", "Compare your health to theirs.", true));
    public final NumberSetting animSpeed = add(new NumberSetting("Bar Animation", "Health bar animation length.", 300, 0, 1000, 25).unit("ms"));

    private LivingEntity target;
    private long lastSeen;
    private final Animation health = new Animation(1, 300);
    private final Animation fade = new Animation(0, 200);

    public TargetHud() {
        super("Target HUD", "Information panel about the entity you are fighting.", Category.COMBAT, 0.55, 0.64);
        alsoListIn(Category.VISUALS);
    }

    private LivingEntity resolve() {
        LivingEntity t = null;
        if (!source.is("Looking")) {
            LivingEntity last = VoidClient.get().combat().lastTarget();
            if (last != null && VoidClient.get().combat().sinceAttackMs() < timeout.get() * 1000) t = last;
        }
        if (t == null && !source.is("Attacked") && mc.crosshairTarget instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le) t = le;
        return t;
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        LivingEntity t = mc.player == null ? null : resolve();
        if (t != null) {
            if (t != target) health.snap(t.getHealth() / Math.max(1, t.getMaxHealth()));
            target = t;
            lastSeen = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastSeen > 600) target = null;
        LivingEntity shown = target != null ? target : editor ? mc.player : null;
        fade.target(shown != null ? 1 : 0);
        float a = fade.get();
        float w = 150, h = 48;
        size(w, h);
        if (shown == null || a < 0.02f) return;
        if (target == null && editor && mc.player == null) return;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(w / 2f, h / 2f);
        m.scale(0.9f + 0.1f * a, 0.9f + 0.1f * a);
        m.translate(-w / 2f, -h / 2f);
        panel(ctx, 0, 0, w, h);
        float hx = 6;
        if (shown instanceof AbstractClientPlayerEntity p) {
            PlayerSkinDrawer.draw(ctx, p.getSkin(), 5, 5, 26);
            hx = 36;
        } else {
            Render2D.roundRect(ctx, 5, 5, 26, 26, 4, ColorUtil.withAlpha(accent(), 90));
            text(ctx, shown.getName().getString().substring(0, 1), 15, 14, 0xFFFFFFFF);
            hx = 36;
        }
        text(ctx, shown.getName().getString(), hx, 5, textColor.getColor());
        float hp = shown.getHealth(), max = Math.max(1, shown.getMaxHealth()), abs = shown.getAbsorptionAmount();
        StringBuilder info = new StringBuilder(String.format(Locale.ROOT, "%.1f HP", hp + abs));
        if (showDistance.get() && mc.player != null && shown != mc.player) info.append(String.format(Locale.ROOT, "  %.1fm", shown.distanceTo(mc.player)));
        if (showPops.get()) {
            int pops = VoidClient.get().combat().pops(shown);
            if (pops > 0) info.append("  -").append(pops).append(" pop");
        }
        text(ctx, info.toString(), hx, 15, dim(textColor.getColor()));
        if (winIndicator.get() && mc.player != null && shown != mc.player) {
            boolean winning = mc.player.getHealth() + mc.player.getAbsorptionAmount() >= hp + abs;
            String wl = winning ? "WIN" : "LOSE";
            text(ctx, wl, w - 6 - textWidth(wl), 5, winning ? 0xFF3CFFA0 : 0xFFFF3C6E);
        }
        float frac = hp / max;
        Animation anim = health;
        anim.target(frac);
        float shownFrac = animSpeed.get() <= 0 ? frac : Math.max(0, Math.min(1, anim.get()));
        float bx = 5, by = 35, bw = w - 10, bh = 6;
        Render2D.roundRect(ctx, bx, by, bw, bh, 3, 0x60000000);
        int hc = ColorUtil.health(frac);
        switch (barStyle.get()) {
            case "Solid" -> Render2D.roundRect(ctx, bx, by, bw * shownFrac, bh, 3, hc);
            case "Segmented" -> {
                int segs = (int) Math.ceil(max / 2);
                float sw = bw / segs;
                for (int i = 0; i < segs; i++) {
                    float f = Math.max(0, Math.min(1, shownFrac * segs - i));
                    if (f > 0) Render2D.rect(ctx, bx + i * sw + 0.5f, by, (sw - 1) * f, bh, hc);
                }
            }
            default -> Render2D.roundRectH(ctx, bx, by, Math.max(bh, bw * shownFrac), bh, 3, ThemeManager.accent(0), hc);
        }
        if (abs > 0) Render2D.roundRect(ctx, bx, by, bw * Math.min(1, abs / max), 2, 1, 0xFFFFD700);
        if (showArmor.get()) {
            float ax = w - 6 - 4 * 13;
            for (int i = 0; i < 4; i++) {
                ItemStack s = shown.getEquippedStack(InventoryUtil.ARMOR[i]);
                if (s.isEmpty()) continue;
                var mm = ctx.getMatrices();
                mm.pushMatrix();
                mm.translate(ax + i * 13, 17);
                mm.scale(0.75f, 0.75f);
                ctx.drawItem(s, 0, 0);
                mm.popMatrix();
            }
        }
        m.popMatrix();
    }

    @Override
    public boolean shouldRender() { return true; }
}
