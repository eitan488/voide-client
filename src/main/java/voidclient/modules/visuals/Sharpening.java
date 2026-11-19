package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.NumberSetting;

public class Sharpening extends Module {
    public static Sharpening INSTANCE;

    public final NumberSetting amount = add(new NumberSetting("Amount", "Unsharp-mask strength.", 0.6, 0, 3, 0.05));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Sample distance in pixels.", 1, 0.5, 3, 0.25).unit("px"));

    public Sharpening() {
        super("Sharpening", "Unsharp-mask sharpening for crisper textures.", Category.VISUALS);
        INSTANCE = this;
    }
}
