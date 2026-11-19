package voidclient.render.duck;

/** Extra per-frame data attached to ItemEntityRenderState for Item Physics. */
public interface ItemEntityRenderStateDuck {
    boolean voidclient$onGround();
    void voidclient$setOnGround(boolean onGround);
    float voidclient$airSpin();
    void voidclient$setAirSpin(float spin);
}
