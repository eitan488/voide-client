package voidclient.modules.visuals;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

/** Brightens the lightmap through the vanilla night-vision path (client-side only). */
public class Fullbright extends Module {
    public static Fullbright INSTANCE;

    public final NumberSetting strength = add(new NumberSetting("Strength", "Brightness boost (1 = full night vision).", 1, 0.1, 1, 0.05));
    public final BooleanSetting fade = add(new BooleanSetting("Fade", "Fade in and out instead of switching instantly.", true));
    private float current;

    public Fullbright() {
        super("Fullbright", "See in the dark without a potion effect.", Category.VISUALS);
        INSTANCE = this;
        // permanent: also runs after disable so the fade-out can finish
        VoidClient.get().events().subscribe(Events.TickPost.class, e -> {
            float target = isEnabled() ? strength.getFloat() : 0f;
            if (!fade.get()) current = target;
            else current += (target - current) * 0.25f;
            if (Math.abs(target - current) < 0.01f) current = target;
        });
    }

    public static boolean active() { return INSTANCE != null && INSTANCE.current > 0.001f; }

    public static float strength() { return INSTANCE == null ? 0 : INSTANCE.current; }
}
