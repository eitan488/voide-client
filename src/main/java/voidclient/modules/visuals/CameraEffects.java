package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;

public class CameraEffects extends Module {
    public static CameraEffects INSTANCE;

    public final BooleanSetting noHurtCam = add(new BooleanSetting("No Hurt Cam", "Disable the camera tilt when you take damage.", true));
    public final BooleanSetting noBob = add(new BooleanSetting("No View Bobbing", "Disable camera bobbing while walking.", false));
    public final NumberSetting fovScale = add(new NumberSetting("Dynamic FOV Scale", "Scale of speed/effect FOV changes (0 = static FOV).", 0.5, 0, 1.5, 0.05).unit("x"));

    public CameraEffects() {
        super("Camera Effects", "Hurt camera, view bobbing and dynamic FOV control.", Category.VISUALS);
        INSTANCE = this;
    }

    public static boolean noHurtCam() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.noHurtCam.get(); }
    public static boolean noBob() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.noBob.get(); }
}
