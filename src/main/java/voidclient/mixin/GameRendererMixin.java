package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.basehunting.Freecam;
import voidclient.modules.visuals.CameraEffects;
import voidclient.render.FovHooks;
import voidclient.shaders.PostProcessingManager;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V", shift = At.Shift.AFTER))
    private void voidclient$postProcess(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        PostProcessingManager.get().render(tickCounter.getTickProgress(true));
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0))
    private void voidclient$depthPass(RenderTickCounter tickCounter, CallbackInfo ci) {
        PostProcessingManager.get().renderDepthPass();
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float voidclient$fov(float original, Camera camera, float tickProgress, boolean changingFov) {
        return FovHooks.modify(original, tickProgress, changingFov);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void voidclient$hurtCam(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (CameraEffects.noHurtCam()) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void voidclient$bob(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (CameraEffects.noBob()) ci.cancel();
    }

    /** No first-person hand while the camera is detached (the rest of renderHand still flushes entities). */
    @ModifyExpressionValue(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"))
    private boolean voidclient$freecamHand(boolean firstPerson) {
        return firstPerson && Freecam.cameraState(0f) == null;
    }
}
