package voidclient.modules.visuals;

import net.minecraft.client.MinecraftClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;

public class ChromaticAberration extends Module {
    public static ChromaticAberration INSTANCE;

    public final NumberSetting strength = add(new NumberSetting("Strength", "RGB channel offset in pixels.", 2, 0, 12, 0.25).unit("px"));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Radial grows towards the edges; Horizontal is a flat split.", "Radial", "Radial", "Horizontal"));
    public final BooleanSetting damagePulse = add(new BooleanSetting("Damage Pulse", "Briefly boosts the effect when you take damage.", true));
    public final NumberSetting pulseStrength = add(new NumberSetting("Pulse Strength", "Extra pixels during the damage pulse.", 6, 0, 20, 0.5).visibleWhen(damagePulse::get));
    public final NumberSetting animation = add(new NumberSetting("Animation", "Slow oscillation of the strength (0 = off).", 0, 0, 2, 0.05));

    public ChromaticAberration() {
        super("Chromatic Aberration", "Lens-style RGB split, optionally pulsing when you get hit.", Category.VISUALS);
        INSTANCE = this;
    }

    public float currentStrength() {
        float s = strength.getFloat();
        if (animation.get() > 0) s *= (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * animation.get()));
        var p = MinecraftClient.getInstance().player;
        if (damagePulse.get() && p != null && p.hurtTime > 0) s += pulseStrength.getFloat() * p.hurtTime / 10f;
        return s;
    }
}
