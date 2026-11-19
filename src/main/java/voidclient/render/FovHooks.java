package voidclient.render;

import net.minecraft.client.MinecraftClient;
import voidclient.modules.basehunting.Freecam;
import voidclient.modules.visuals.CameraEffects;
import voidclient.modules.visuals.Zoom;

/** Combines FOV changes from Camera Effects, Freecam and Zoom (applied in that order). */
public final class FovHooks {
    private FovHooks() {}

    public static float modify(float fov, float tickProgress, boolean changingFov) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float base = mc.options.getFov().getValue();
        float out = fov;
        CameraEffects ce = CameraEffects.INSTANCE;
        if (ce != null && ce.isEnabled() && changingFov) out = base + (fov - base) * ce.fovScale.getFloat();
        Freecam fc = Freecam.INSTANCE;
        if (fc != null && fc.isEnabled() && fc.fov.get() > 0) out = fc.fov.getFloat();
        Zoom z = Zoom.INSTANCE;
        if (z != null) out = z.apply(out);
        return out;
    }
}
