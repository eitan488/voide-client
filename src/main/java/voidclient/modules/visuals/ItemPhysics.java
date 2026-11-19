package voidclient.modules.visuals;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;

/** Dropped items lie flat on the ground and tumble while falling. */
public class ItemPhysics extends Module {
    public static ItemPhysics INSTANCE;

    public final BooleanSetting tumble = add(new BooleanSetting("Tumble In Air", "Items spin end-over-end while airborne.", true));
    public final NumberSetting spinSpeed = add(new NumberSetting("Spin Speed", "Airborne tumble speed.", 1, 0.1, 4, 0.1).visibleWhen(tumble::get));
    public final NumberSetting scale = add(new NumberSetting("Item Scale", "Size of dropped items.", 1, 0.5, 2, 0.05));

    public ItemPhysics() {
        super("Item Physics", "Realistic resting and tumbling for dropped items.", Category.VISUALS);
        INSTANCE = this;
    }

    public static boolean active() { return INSTANCE != null && INSTANCE.isEnabled(); }
    public static boolean tumble() { return INSTANCE.tumble.get(); }
    public static float spinSpeed() { return INSTANCE.spinSpeed.getFloat(); }
    public static float scale() { return INSTANCE.scale.getFloat(); }
}
