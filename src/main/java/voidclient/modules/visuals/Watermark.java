package voidclient.modules.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.StringSetting;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** VOID CLIENT branding with optional version, FPS, time and server. */
public class Watermark extends HudModule {
    private static final Identifier LOGO = Identifier.of("voidclient", "textures/gui/logo.png");

    public final ModeSetting style = add(new ModeSetting("Style", "Watermark style.", "Text", "Text", "Logo", "Minimal"));
    public final StringSetting customText = add(new StringSetting("Text", "Brand text.", "VOID", 24));
    public final BooleanSetting version = add(new BooleanSetting("Version", "Show the client version.", true));
    public final BooleanSetting fps = add(new BooleanSetting("FPS", "Show FPS.", true));
    public final BooleanSetting time = add(new BooleanSetting("Time", "Show the time.", false));
    public final BooleanSetting server = add(new BooleanSetting("Server", "Show the server address.", false));

    public Watermark() {
        super("Watermark", "Void Client branding on the HUD.", Category.VISUALS, 0.005, 0.005);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        StringBuilder info = new StringBuilder();
        if (version.get()) info.append(" ").append(VoidClient.VERSION);
        if (fps.get()) info.append("  ").append(mc.getCurrentFps()).append(" fps");
        if (time.get()) info.append("  ").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (server.get() && mc.getCurrentServerEntry() != null) info.append("  ").append(mc.getCurrentServerEntry().address);
        switch (style.get()) {
            case "Logo" -> {
                float w = 72, h = 18;
                float iw = textWidth(info.toString());
                size(w + iw + 6, h + 4);
                panel(ctx, 0, 0, w + iw + 6, h + 4);
                Render2D.texture(ctx, LOGO, 0, 2, w, h, 0xFFFFFFFF);
                text(ctx, info.toString().trim(), w, 7, dim(textColor.getColor()));
            }
            case "Minimal" -> {
                String s = customText.get() + info;
                size(textWidth(s) + 2, 10);
                text(ctx, s, 1, 1, ThemeManager.accent(0));
            }
            default -> {
                float bw = FontRenderer.width(customText.get(), FontRenderer.Family.INTER_BOLD);
                float w = bw + textWidth(info.toString()) + 12, h = 15;
                size(w, h);
                panel(ctx, 0, 0, w, h);
                FontRenderer.drawGradient(ctx, customText.get(), 5, 4, ThemeManager.accent(0), ThemeManager.accent(0.33f), FontRenderer.Family.INTER_BOLD, font.shadow());
                text(ctx, info.toString(), 6 + bw, 4, dim(textColor.getColor()));
                Render2D.rect(ctx, 4, h - 2, w - 8, 1, ThemeManager.accent(0.5f));
            }
        }
    }
}
