package voidclient.modules.render;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.Notifications;
import voidclient.modules.visuals.Bloom;
import voidclient.modules.visuals.ColorGrading;
import voidclient.modules.visuals.DepthEffects;
import voidclient.modules.visuals.FilmGrain;
import voidclient.modules.visuals.MotionBlur;
import voidclient.modules.visuals.Vignette;

/** Performance and visual presets: Potato, Low, Balanced, High, Ultra, Cinematic. */
public class VisualPresets extends Module {
    public final ModeSetting preset = add(new ModeSetting("Preset", "Preset applied when you press Apply.", "Balanced", "Potato", "Low", "Balanced", "High", "Ultra", "Cinematic"));
    public final BooleanSetting vanilla = add(new BooleanSetting("Apply Vanilla Options", "Render distance, graphics, particles, clouds, entity distance.", true));
    public final BooleanSetting effects = add(new BooleanSetting("Apply Void Effects", "Toggle and tune Void post effects.", true));

    public VisualPresets() {
        super("Visual Preset Manager", "One-click performance and visual presets (Potato to Cinematic).", Category.RENDER);
        alsoListIn(Category.VISUALS);
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() {
        String p = preset.get();
        int level = preset.index();
        if (vanilla.get()) {
            var o = mc.options;
            int[] dist = {4, 6, 10, 14, 20, 16};
            o.getViewDistance().setValue(dist[level]);
            o.getPreset().setValue(level <= 1 ? GraphicsMode.FAST : level >= 4 ? GraphicsMode.FABULOUS : GraphicsMode.FANCY);
            o.getParticles().setValue(level == 0 ? ParticlesMode.MINIMAL : level <= 2 ? ParticlesMode.DECREASED : ParticlesMode.ALL);
            o.getCloudRenderMode().setValue(level <= 1 ? CloudRenderMode.OFF : level == 2 ? CloudRenderMode.FAST : CloudRenderMode.FANCY);
            o.getEntityDistanceScaling().setValue(level <= 1 ? 0.5 : level == 2 ? 0.75 : 1.0);
            o.write();
        }
        if (effects.get()) {
            var mm = VoidClient.get().modules();
            boolean fx = level >= 3;
            mm.get(Bloom.class).setEnabled(fx);
            mm.get(Bloom.class).quality.set(level == 3 ? "Medium" : level == 4 ? "High" : "High");
            mm.get(ColorGrading.class).setEnabled(level >= 2);
            mm.get(ColorGrading.class).preset.set(level == 5 ? "Cinematic" : "Neutral");
            mm.get(Vignette.class).setEnabled(level >= 4);
            mm.get(FilmGrain.class).setEnabled(level == 5);
            mm.get(MotionBlur.class).setEnabled(level == 5);
            mm.get(DepthEffects.class).setEnabled(level == 5);
            mm.get(CustomParticleQuality.class).setEnabled(level <= 1);
        }
        Notifications.success("Visual preset", p + " applied");
    }

    @Override
    public String getInfo() { return preset.get(); }
}
