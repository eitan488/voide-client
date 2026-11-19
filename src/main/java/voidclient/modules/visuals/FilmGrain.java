package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;

public class FilmGrain extends Module {
    public static FilmGrain INSTANCE;

    public final NumberSetting intensity = add(new NumberSetting("Intensity", "Grain strength.", 0.06, 0, 0.4, 0.005));
    public final NumberSetting size = add(new NumberSetting("Grain Size", "Size of a grain cell in pixels.", 1.5, 1, 6, 0.25).unit("px"));
    public final BooleanSetting animated = add(new BooleanSetting("Animated", "Re-seed the grain 24 times per second.", true));
    public final NumberSetting luminanceResponse = add(new NumberSetting("Luminance Response", "Reduce grain in bright areas.", 0.5, 0, 1, 0.05));

    public FilmGrain() {
        super("Film Grain", "Animated photographic grain.", Category.VISUALS);
        INSTANCE = this;
    }
}
