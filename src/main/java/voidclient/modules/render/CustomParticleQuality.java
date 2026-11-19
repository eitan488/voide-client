package voidclient.modules.render;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.NumberSetting;
import voidclient.particles.ParticleManager;

import java.util.concurrent.ThreadLocalRandom;

/** Global particle density for vanilla particles and the cap for Void particles. */
public class CustomParticleQuality extends Module {
    public static CustomParticleQuality INSTANCE;

    public final NumberSetting density = add(new NumberSetting("Vanilla Density", "Percentage of vanilla particles kept.", 50, 0, 100, 5).unit("%"));
    public final NumberSetting blockBreak = add(new NumberSetting("Block Break Density", "Percentage of block-break particles kept.", 30, 0, 100, 5).unit("%"));
    public final NumberSetting voidCap = add(new NumberSetting("Void Particle Cap", "Maximum Void client particles alive.", 1500, 50, 4000, 50));

    public CustomParticleQuality() {
        super("Custom Particle Quality", "Particle density and limits for performance or style.", Category.RENDER);
        INSTANCE = this;
        voidCap.onChange(v -> ParticleManager.get().setCap(v.intValue()));
    }

    @Override
    protected void onEnable() { ParticleManager.get().setCap(voidCap.getInt()); }

    @Override
    protected void onDisable() { ParticleManager.get().setCap(1500); }

    public static boolean allow(ParticleEffect effect) {
        CustomParticleQuality q = INSTANCE;
        if (q == null || !q.isEnabled()) return true;
        double keep = (effect.getType() == ParticleTypes.BLOCK ? q.blockBreak.get() : q.density.get()) / 100.0;
        return keep >= 1 || ThreadLocalRandom.current().nextDouble() < keep;
    }

    @Override
    public String getInfo() { return density.getInt() + "%"; }
}
