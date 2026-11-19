package voidclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.VoidClient;
import voidclient.gui.screens.LoadingScreenRenderer;

/**
 * Void loading overlay. Keeps vanilla's timing/fade logic but draws only vector shapes,
 * because textures and fonts may not be loaded yet during the first resource reload.
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void voidclient$render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (VoidClient.get() == null || !VoidClient.get().clientSettings().customLoadingScreen.get()) return;
        ci.cancel();
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long now = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) reloadStartTime = now;
        float f = reloadCompleteTime > -1L ? (now - reloadCompleteTime) / 1000f : -1f;
        float g = reloadStartTime > -1L ? (now - reloadStartTime) / 500f : -1f;
        float alpha;
        if (f >= 1f) {
            if (client.currentScreen != null) client.currentScreen.renderWithTooltip(ctx, 0, 0, delta);
            else client.inGameHud.renderDeferredSubtitles();
            alpha = 1f - MathHelper.clamp(f - 1f, 0f, 1f);
            ctx.createNewRootLayer();
            LoadingScreenRenderer.background(ctx, w, h, alpha);
        } else if (reloading) {
            if (client.currentScreen != null && g < 1f) client.currentScreen.renderWithTooltip(ctx, mouseX, mouseY, delta);
            else client.inGameHud.renderDeferredSubtitles();
            alpha = MathHelper.clamp(g, 0f, 1f);
            ctx.createNewRootLayer();
            LoadingScreenRenderer.background(ctx, w, h, (float) MathHelper.clamp(g, 0.15, 1.0));
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(client.getFramebuffer().getColorAttachment(), LoadingScreenRenderer.BACKGROUND);
            alpha = 1f;
        }
        float target = reload.getProgress();
        progress = MathHelper.clamp(progress * 0.95f + target * 0.050000012f, 0f, 1f);
        float barAlpha = f < 1f ? 1f - MathHelper.clamp(f, 0f, 1f) : 0f;
        LoadingScreenRenderer.foreground(ctx, w, h, alpha, progress, barAlpha);
        if (f >= 2f) client.setOverlay(null);
    }
}
