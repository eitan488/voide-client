package voidclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.HudModule;
import voidclient.gui.Notifications;
import voidclient.gui.screens.HudEditorScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Draws enabled HUD modules ordered by layer, then notifications. */
public final class HudManager {
    private final List<HudModule> sorted = new ArrayList<>();
    private boolean hidden;

    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isHidden() { return hidden; }

    public List<HudModule> sortedElements() {
        sorted.clear();
        sorted.addAll(VoidClient.get().modules().hudModules());
        sorted.sort(Comparator.comparingDouble(h -> h.layer.get()));
        return sorted;
    }

    public void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        if (!hidden && !mc.options.hudHidden && !(mc.currentScreen instanceof HudEditorScreen) && !mc.getDebugHud().shouldShowDebugHud()
                && !voidclient.modules.basehunting.Freecam.hidesClientHud()) {
            for (HudModule h : sortedElements()) {
                if (!h.isEnabled()) continue;
                try {
                    h.render(ctx, sw, sh, delta, false);
                } catch (Throwable t) {
                    h.onListenerError(t);
                }
            }
        }
        Notifications.render(ctx, sw, sh);
    }
}
