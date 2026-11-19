package voidclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.visuals.CustomFireVisuals;
import voidclient.modules.visuals.CustomWaterVisuals;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @WrapOperation(method = "renderOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V"))
    private void voidclient$fire(MatrixStack matrices, VertexConsumerProvider consumers, Sprite sprite, Operation<Void> original) {
        if (!CustomFireVisuals.active()) {
            original.call(matrices, consumers, sprite);
            return;
        }
        if (CustomFireVisuals.hidden()) return;
        matrices.push();
        matrices.translate(0f, -CustomFireVisuals.lowerBy(), 0f);
        original.call(matrices, consumers, sprite);
        matrices.pop();
    }

    @WrapOperation(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer voidclient$fireAlpha(VertexConsumer vc, float r, float g, float b, float a, Operation<VertexConsumer> original) {
        if (!CustomFireVisuals.active()) return original.call(vc, r, g, b, a);
        int tint = CustomFireVisuals.tint();
        return original.call(vc, r * ((tint >> 16) & 255) / 255f, g * ((tint >> 8) & 255) / 255f, b * (tint & 255) / 255f, CustomFireVisuals.opacity());
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void voidclient$water(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider consumers, CallbackInfo ci) {
        if (CustomWaterVisuals.hidesOverlay()) ci.cancel();
    }
}
