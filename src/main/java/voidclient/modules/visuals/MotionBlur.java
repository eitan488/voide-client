package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;

/** Frame-accumulation motion blur. */
public class MotionBlur extends Module {
    public static MotionBlur INSTANCE;

    public final NumberSetting strength = add(new NumberSetting("Strength", "How much of the previous frames is kept.", 0.55, 0.05, 0.95, 0.01));
    public final BooleanSetting frameIndependent = add(new BooleanSetting("FPS Independent", "Keep the trail length the same at any frame rate (reference 60 FPS).", true));

    public MotionBlur() {
        super("Motion Blur", "Accumulation motion blur using a history buffer.", Category.VISUALS);
        INSTANCE = this;
    }

    public float historyWeight(float dtSeconds) {
        float s = strength.getFloat();
        if (!frameIndependent.get() || dtSeconds <= 0) return s;
        return (float) Math.min(0.97, Math.pow(s, dtSeconds * 60.0));
    }
}
