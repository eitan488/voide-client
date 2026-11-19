package voidclient.modules.visuals;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.Notifications;

/** Applies coordinated presets to every Void particle effect at once. */
public class ParticlePresets extends Module {
    public final ModeSetting preset = add(new ModeSetting("Preset", "Preset to apply.", "Void Storm", "Void Storm", "Ember Glow", "Frost", "Cyber Rain", "Minimal"));
    public final BooleanSetting enableEffects = add(new BooleanSetting("Enable Effects", "Also enable the effects used by the preset.", true));

    public ParticlePresets() {
        super("Particle Presets", "One-click styles for all Void particle effects.", Category.VISUALS);
    }

    @Override
    public boolean isToggleable() { return false; }

    private void style(ParticleModule m, String type, int color, double size, int life, double gravity, double density, int amount) {
        m.type.set(type);
        m.color.setArgb(color);
        m.color.setThemed(false);
        m.size.set(size);
        m.lifetime.set((double) life);
        m.gravity.set(gravity);
        m.density.set(density);
        m.amount.set((double) amount);
    }

    @Override
    protected void runAction() {
        var mm = VoidClient.get().modules();
        CustomParticles cp = mm.get(CustomParticles.class);
        AmbientParticles ap = mm.get(AmbientParticles.class);
        HitParticles hp = mm.get(HitParticles.class);
        TrailEffects tr = mm.get(TrailEffects.class);
        switch (preset.get()) {
            case "Ember Glow" -> {
                style(cp, "Embers", 0xFFFF7A2E, 0.06, 40, -0.001, 0.5, 3);
                style(ap, "Embers", 0xFFFFB04A, 0.05, 90, -0.0008, 0.4, 2);
                style(hp, "Embers", 0xFFFF5E3A, 0.08, 25, 0.002, 1, 18);
                style(tr, "Embers", 0xFFFF7A2E, 0.05, 30, 0, 0.6, 2);
            }
            case "Frost" -> {
                style(cp, "Snow", 0xFFCFF4FF, 0.05, 50, 0.001, 0.5, 3);
                style(ap, "Snow", 0xFFE8FBFF, 0.04, 140, 0.0006, 0.7, 4);
                style(hp, "Sparkles", 0xFF9FE6FF, 0.07, 22, 0.002, 1, 16);
                style(tr, "Snow", 0xFFBDEBFF, 0.05, 30, 0, 0.6, 2);
            }
            case "Cyber Rain" -> {
                style(cp, "Neon Dust", 0xFF00F0FF, 0.05, 25, 0.01, 0.7, 4);
                style(ap, "Neon Dust", 0xFF00F0FF, 0.03, 60, 0.02, 0.9, 6);
                style(hp, "Neon Dust", 0xFFFF2BD6, 0.07, 18, 0.003, 1, 20);
                style(tr, "Neon Dust", 0xFF00F0FF, 0.04, 20, 0, 0.6, 2);
            }
            case "Minimal" -> {
                style(cp, "Sparkles", 0x80FFFFFF, 0.03, 20, 0, 0.2, 1);
                style(ap, "Stars", 0x60FFFFFF, 0.03, 80, -0.0002, 0.2, 1);
                style(hp, "Sparkles", 0xC0FFFFFF, 0.05, 14, 0.002, 1, 8);
                style(tr, "Sparkles", 0x80FFFFFF, 0.03, 20, 0, 0.3, 1);
            }
            default -> {
                style(cp, "Neon Dust", 0xFFB026FF, 0.08, 30, 0.002, 0.6, 4);
                style(ap, "Stars", 0xFF7A5CFF, 0.05, 120, -0.0005, 0.6, 3);
                style(hp, "Neon Dust", 0xFF00F0FF, 0.08, 25, 0.002, 1, 16);
                style(tr, "Neon Dust", 0xFFB026FF, 0.06, 30, 0, 0.6, 2);
            }
        }
        if (enableEffects.get()) {
            cp.setEnabled(true);
            ap.setEnabled(true);
            hp.setEnabled(true);
            tr.setEnabled(true);
        }
        Notifications.success("Particle preset", preset.get());
    }
}
