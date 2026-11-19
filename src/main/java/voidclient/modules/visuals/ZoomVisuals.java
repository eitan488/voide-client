package voidclient.modules.visuals;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Scope overlay, zoom vignette and zoom level readout while the Zoom module is active. */
public class ZoomVisuals extends Module {
    public static ZoomVisuals INSTANCE;

    public final BooleanSetting vignette = add(new BooleanSetting("Vignette", "Shader vignette that closes in as you zoom.", true));
    public final NumberSetting vignetteStrength = add(new NumberSetting("Vignette Strength", "Vignette darkness at full zoom.", 0.8, 0, 1, 0.05).visibleWhen(vignette::get));
    public final ModeSetting overlay = add(new ModeSetting("Scope Overlay", "Overlay drawn while zoomed.", "Reticle", "None", "Circle", "Reticle"));
    public final ColorSetting overlayColor = add(new ColorSetting("Overlay Color", "Scope overlay colour.", 0xCC00F0FF).themed());
    public final BooleanSetting showLevel = add(new BooleanSetting("Show Zoom Level", "Display the current magnification.", true));

    public ZoomVisuals() {
        super("Zoom Visuals", "Scope overlay, vignette and zoom readout for the Zoom module.", Category.VISUALS);
        INSTANCE = this;
        on(Events.HudRender.class, this::draw);
    }

    public float progress() {
        Zoom z = Zoom.INSTANCE;
        return z == null ? 0 : z.progress();
    }

    private void draw(Events.HudRender e) {
        float p = progress();
        if (p <= 0.01f) return;
        DrawContext ctx = e.ctx;
        float cx = e.width / 2f, cy = e.height / 2f;
        int c = ColorUtil.multiplyAlpha(overlayColor.getColor(), p);
        float r = Math.min(e.width, e.height) * 0.32f;
        switch (overlay.get()) {
            case "Circle" -> Render2D.ring(ctx, cx, cy, r, 1.2f, c);
            case "Reticle" -> {
                Render2D.ring(ctx, cx, cy, r, 1f, c);
                Render2D.line(ctx, cx - r, cy, cx - 8, cy, 1f, c);
                Render2D.line(ctx, cx + 8, cy, cx + r, cy, 1f, c);
                Render2D.line(ctx, cx, cy - r, cx, cy - 8, 1f, c);
                Render2D.line(ctx, cx, cy + 8, cx, cy + r, 1f, c);
                for (int i = 1; i <= 4; i++) {
                    float d = r * i / 5f;
                    Render2D.rect(ctx, cx + d - 0.5f, cy - 3, 1, 6, c);
                    Render2D.rect(ctx, cx - d - 0.5f, cy - 3, 1, 6, c);
                }
            }
            default -> {
            }
        }
        if (showLevel.get() && Zoom.INSTANCE != null) {
            String s = String.format(Locale.ROOT, "%.1fx", Zoom.INSTANCE.currentZoom());
            FontRenderer.draw(ctx, s, cx + 12, cy + 12, c, FontRenderer.Family.MONO, true);
        }
    }
}
