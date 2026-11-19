package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.modules.visuals.NameTags;
import voidclient.render.EntityOutlines;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void voidclient$outlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (state.outlineColor != 0) {
            int c = EntityOutlines.color(entity);
            if (c != 0) state.outlineColor = c | 0xFF000000;
        }
    }

    @ModifyReturnValue(method = "hasLabel", at = @At("RETURN"))
    private boolean voidclient$hideVanillaLabel(boolean original, T entity, double squaredDistanceToCamera) {
        return original && !NameTags.replacesVanilla(entity);
    }
}
