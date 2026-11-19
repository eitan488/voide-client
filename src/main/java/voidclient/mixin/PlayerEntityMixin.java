package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import voidclient.modules.movement.SafeWalk;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @ModifyReturnValue(method = "clipAtLedge", at = @At("RETURN"))
    private boolean voidclient$safeWalk(boolean original) {
        return original || ((Object) this == MinecraftClient.getInstance().player && SafeWalk.shouldClip());
    }
}
