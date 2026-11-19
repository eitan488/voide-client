package voidclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.basehunting.Freecam;
import voidclient.modules.visuals.Zoom;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void voidclient$look(double dx, double dy, CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player && Freecam.onLook(dx, dy)) ci.cancel();
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double voidclient$zoomYaw(double dx) {
        return dx * voidclient$lookScale();
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double voidclient$zoomPitch(double dy) {
        return dy * voidclient$lookScale();
    }

    private double voidclient$lookScale() {
        Zoom z = Zoom.INSTANCE;
        return z != null && (Object) this == MinecraftClient.getInstance().player ? z.lookScale() : 1.0;
    }
}
