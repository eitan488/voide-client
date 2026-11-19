package voidclient.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.VoidClient;
import voidclient.events.Events;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void voidclient$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (VoidClient.get() == null) return;
        Events.MouseButton e = VoidClient.get().events().post(new Events.MouseButton(input.button(), action));
        if (e.isCancelled()) ci.cancel();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void voidclient$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (VoidClient.get() == null || net.minecraft.client.MinecraftClient.getInstance().currentScreen != null) return;
        Events.MouseScroll e = VoidClient.get().events().post(new Events.MouseScroll(vertical));
        if (e.isCancelled()) ci.cancel();
    }
}
