package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import voidclient.modules.render.AnimationSettings;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
    private int voidclient$swing(int original) {
        if ((Object) this != MinecraftClient.getInstance().player) return original;
        return AnimationSettings.swingDuration(original);
    }
}
