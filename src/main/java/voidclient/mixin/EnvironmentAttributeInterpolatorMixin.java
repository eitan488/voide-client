package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import voidclient.render.VisualOverrides;

/** Single hook for every visual sky/fog/light attribute in 1.21.11. */
@Mixin(EnvironmentAttributeInterpolator.class)
public abstract class EnvironmentAttributeInterpolatorMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private <V> V voidclient$override(V original, EnvironmentAttribute<V> attribute, float tickProgress) {
        return VisualOverrides.apply(attribute, original);
    }
}
