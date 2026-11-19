package voidclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import voidclient.render.duck.ItemEntityRenderStateDuck;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.visuals.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void voidclient$capture(ItemEntity entity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        ItemEntityRenderStateDuck duck = (ItemEntityRenderStateDuck) state;
        duck.voidclient$setOnGround(entity.isOnGround() || entity.isTouchingWater());
        duck.voidclient$setAirSpin((entity.age + tickProgress) * 12f + entity.uniqueOffset * 57.3f);
    }

    /** Replace the bobbing lift with a resting offset when lying flat. */
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0))
    private void voidclient$lift(MatrixStack matrices, float x, float y, float z, Operation<Void> original, ItemEntityRenderState state) {
        if (!ItemPhysics.active()) {
            original.call(matrices, x, y, z);
            return;
        }
        var box = state.itemRenderState.getModelBoundingBox();
        boolean flat = box.getLengthZ() <= 0.0625;
        float rest = flat ? 0.02f : (float) -box.minY;
        original.call(matrices, 0f, rest, 0f);
    }

    /** Replace the spin with a stable yaw plus a flat pitch (or a tumble while airborne). */
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V", ordinal = 0))
    private void voidclient$rotate(MatrixStack matrices, Quaternionfc rotation, Operation<Void> original, ItemEntityRenderState state) {
        if (!ItemPhysics.active()) {
            original.call(matrices, rotation);
            return;
        }
        ItemEntityRenderStateDuck duck = (ItemEntityRenderStateDuck) state;
        var box = state.itemRenderState.getModelBoundingBox();
        boolean flat = box.getLengthZ() <= 0.0625;
        float yaw = state.uniqueOffset * 57.2958f % 360f;
        original.call(matrices, RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        if (!duck.voidclient$onGround() && ItemPhysics.tumble()) {
            original.call(matrices, RotationAxis.POSITIVE_X.rotationDegrees(duck.voidclient$airSpin() * ItemPhysics.spinSpeed()));
        } else if (flat) {
            original.call(matrices, RotationAxis.POSITIVE_X.rotationDegrees(90f));
        }
        float s = ItemPhysics.scale();
        if (s != 1f) matrices.scale(s, s, s);
    }
}
