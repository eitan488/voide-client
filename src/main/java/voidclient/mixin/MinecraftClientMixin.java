package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import voidclient.gui.screens.ScreenReplacer;
import voidclient.modules.basehunting.Freecam;
import voidclient.render.EntityOutlines;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen voidclient$replaceScreen(Screen screen) {
        return ScreenReplacer.replace(screen);
    }

    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    private boolean voidclient$hasOutline(boolean original, Entity entity) {
        return original || EntityOutlines.shouldOutline(entity);
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void voidclient$doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.blocksInteraction()) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void voidclient$doItemUse(CallbackInfo ci) {
        if (Freecam.blocksInteraction()) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void voidclient$handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (Freecam.blocksInteraction()) ci.cancel();
    }
}
