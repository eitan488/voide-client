package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import voidclient.modules.basehunting.Freecam;
import voidclient.modules.render.EntityRendering;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private <E extends Entity> boolean voidclient$cull(boolean original, E entity, Frustum frustum, double x, double y, double z) {
        return original && EntityRendering.shouldRender(entity) && !Freecam.hidesEntity(entity);
    }
}
