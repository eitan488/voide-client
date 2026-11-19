package voidclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.player.PotionHud;
import voidclient.modules.visuals.CustomCrosshair;
import voidclient.modules.render.ScreenshotMode;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void voidclient$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CustomCrosshair.replacesVanilla() || ScreenshotMode.hidesCrosshair()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void voidclient$effects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (PotionHud.hidesVanilla()) ci.cancel();
    }
}
