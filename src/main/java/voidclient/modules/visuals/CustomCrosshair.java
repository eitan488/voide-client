package voidclient.modules.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;

/** Fully configurable crosshair replacing the vanilla one. */
public class CustomCrosshair extends Module {
    public static CustomCrosshair INSTANCE;

    public final ModeSetting style = add(new ModeSetting("Style", "Crosshair shape.", "Cross", "Cross", "Dot", "Circle", "Cross + Dot", "Chevron", "Square", "T-Shape"));
    public final NumberSetting gap = add(new NumberSetting("Gap", "Space between the arms and the centre.", 2, 0, 12, 0.5));
    public final NumberSetting length = add(new NumberSetting("Length", "Arm length.", 5, 1, 20, 0.5));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", "Line thickness.", 1, 0.5, 5, 0.25));
    public final ColorSetting color = add(new ColorSetting("Color", "Crosshair colour.", 0xFFFFFFFF));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", "Dark outline for visibility.", true));
    public final ColorSetting outlineColor = add(new ColorSetting("Outline Color", "Outline colour.", 0xC0000000).visibleWhen(outline::get));
    public final BooleanSetting dynamicGap = add(new BooleanSetting("Dynamic Gap", "Spread while moving or on attack cooldown.", false));
    public final BooleanSetting targetColor = add(new BooleanSetting("Target Color", "Change colour when aiming at a living entity.", true));
    public final ColorSetting targetColorValue = add(new ColorSetting("Target Color Value", "Colour while aiming at an entity.", 0xFFFF3C6E).visibleWhen(targetColor::get));
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Show In Third Person", "Draw the crosshair in third person.", false));

    public CustomCrosshair() {
        super("Custom Crosshair", "Replace the vanilla crosshair with a configurable one.", Category.VISUALS);
        INSTANCE = this;
        on(Events.HudRender.class, e -> {
            if (e.hudHidden || !visible()) return;
            boolean target = targetColor.get() && mc.crosshairTarget instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity;
            float g = gap.getFloat();
            if (dynamicGap.get() && mc.player != null) {
                g += (float) Math.min(4, mc.player.getVelocity().horizontalLength() * 20);
                g += (1f - mc.player.getAttackCooldownProgress(0f)) * 4;
            }
            draw(e.ctx, e.width / 2f, e.height / 2f, g, target ? targetColorValue.getColor() : color.getColor());
        });
    }

    private boolean visible() {
        if (mc.options == null) return false;
        return mc.options.getPerspective().isFirstPerson() || thirdPerson.get();
    }

    public static boolean replacesVanilla() { return INSTANCE != null && INSTANCE.isEnabled(); }

    public void draw(DrawContext ctx, float cx, float cy, float g, int c) {
        float l = length.getFloat(), t = thickness.getFloat();
        if (outline.get()) shape(ctx, cx, cy, g, l, t, outlineColor.getColor(), 1f);
        shape(ctx, cx, cy, g, l, t, c, 0f);
    }

    private void shape(DrawContext ctx, float cx, float cy, float g, float l, float t, int c, float grow) {
        float h = t / 2f + grow;
        switch (style.get()) {
            case "Dot" -> Render2D.circle(ctx, cx, cy, t + grow, c);
            case "Circle" -> Render2D.ring(ctx, cx, cy, g + l / 2f, t + grow * 2, c);
            case "Chevron" -> {
                Render2D.line(ctx, cx - l, cy + g + l * 0.6f, cx, cy + g, t + grow * 2, c);
                Render2D.line(ctx, cx, cy + g, cx + l, cy + g + l * 0.6f, t + grow * 2, c);
            }
            case "Square" -> Render2D.outline(ctx, cx - g - h, cy - g - h, (g + h) * 2, (g + h) * 2, t + grow, c);
            default -> {
                boolean top = !style.is("T-Shape");
                Render2D.rect(ctx, cx - g - l - grow, cy - h, l + grow * 2, h * 2, c);
                Render2D.rect(ctx, cx + g - grow, cy - h, l + grow * 2, h * 2, c);
                if (top) Render2D.rect(ctx, cx - h, cy - g - l - grow, h * 2, l + grow * 2, c);
                Render2D.rect(ctx, cx - h, cy + g - grow, h * 2, l + grow * 2, c);
                if (style.is("Cross + Dot")) Render2D.circle(ctx, cx, cy, t * 0.8f + grow, c);
            }
        }
    }

    @Override
    public boolean hasPreview() { return true; }

    @Override
    public void renderPreview(DrawContext ctx, float x, float y, float w, float h, float delta) {
        draw(ctx, x + w / 2f, y + h / 2f + 4, gap.getFloat(), color.getColor());
    }

    @Override
    public String getInfo() { return style.get(); }
}
