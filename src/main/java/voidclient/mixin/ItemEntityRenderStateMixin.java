package voidclient.mixin;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import voidclient.render.duck.ItemEntityRenderStateDuck;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemEntityRenderStateDuck {
    @Unique private boolean voidclient$onGround;
    @Unique private float voidclient$airSpin;

    @Override public boolean voidclient$onGround() { return voidclient$onGround; }
    @Override public void voidclient$setOnGround(boolean v) { voidclient$onGround = v; }
    @Override public float voidclient$airSpin() { return voidclient$airSpin; }
    @Override public void voidclient$setAirSpin(float v) { voidclient$airSpin = v; }
}
