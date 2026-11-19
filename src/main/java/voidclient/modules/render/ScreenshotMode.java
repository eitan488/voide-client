package voidclient.modules.render;

import net.minecraft.client.util.ScreenshotRecorder;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.keybinds.Keybind;
import voidclient.render.Render2D;

/** Clean screenshot mode: hides HUDs/crosshair, optional letterbox and a capture key. */
public class ScreenshotMode extends Module {
    public static ScreenshotMode INSTANCE;

    public final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla HUD", "Hide the vanilla HUD and hand (like F1).", true));
    public final BooleanSetting hideVoid = add(new BooleanSetting("Hide Void HUD", "Hide every Void HUD element.", true));
    public final BooleanSetting hideCrosshair = add(new BooleanSetting("Hide Crosshair", "Hide the crosshair when the vanilla HUD stays visible.", true).visibleWhen(() -> !hideVanilla.get()));
    public final BooleanSetting letterbox = add(new BooleanSetting("Letterbox", "Cinema bars.", false));
    public final NumberSetting letterboxSize = add(new NumberSetting("Letterbox Size", "Bar height as screen fraction.", 0.1, 0.02, 0.25, 0.01).visibleWhen(letterbox::get));
    public final KeybindSetting captureKey = add(new KeybindSetting("Capture Key", "Take a screenshot while in this mode.", Keybind.NONE));
    private boolean prevHud;

    public ScreenshotMode() {
        super("Screenshot Mode", "Hide UI for clean shots, with letterbox and capture key.", Category.RENDER);
        INSTANCE = this;
        on(Events.Key.class, e -> {
            if (e.action == 1 && mc.currentScreen == null && captureKey.get().matchesKey(e.key)) {
                ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), text -> mc.execute(() -> mc.inGameHud.getChatHud().addMessage(text)));
            }
        });
        on(Events.TickPost.class, e -> {
            if (mc.options != null && hideVanilla.get()) mc.options.hudHidden = true;
            VoidClient.get().hud().setHidden(hideVoid.get());
        });
        on(Events.HudRender.class, e -> {
            if (!letterbox.get()) return;
            float bar = e.height * letterboxSize.getFloat();
            Render2D.rect(e.ctx, 0, 0, e.width, bar, 0xFF000000);
            Render2D.rect(e.ctx, 0, e.height - bar, e.width, bar, 0xFF000000);
        });
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) prevHud = mc.options.hudHidden;
    }

    @Override
    protected void onDisable() {
        if (mc.options != null && hideVanilla.get()) mc.options.hudHidden = prevHud;
        VoidClient.get().hud().setHidden(false);
    }


    public static boolean hidesCrosshair() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideCrosshair.get(); }
}
