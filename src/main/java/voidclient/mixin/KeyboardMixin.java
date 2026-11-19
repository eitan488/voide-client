package voidclient.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.VoidClient;
import voidclient.events.Events;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void voidclient$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (VoidClient.get() == null) return;
        Events.Key e = VoidClient.get().events().post(new Events.Key(input.key(), input.scancode(), action, input.modifiers()));
        if (e.isCancelled()) ci.cancel();
    }
}
