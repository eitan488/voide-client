package voidclient.shaders;

import com.mojang.blaze3d.buffers.Std140Builder;
import voidclient.modules.visuals.ChromaticAberration;
import voidclient.modules.visuals.ColorGrading;
import voidclient.modules.visuals.FilmGrain;
import voidclient.modules.visuals.ScreenEffects;
import voidclient.modules.visuals.ScreenTint;
import voidclient.modules.visuals.Sharpening;
import voidclient.modules.visuals.Vignette;
import voidclient.modules.visuals.ZoomVisuals;
import voidclient.utilities.ColorUtil;

/** Snapshot of every effect that runs inside the uber shader for this frame. */
final class UberParams {
    boolean grading, tint, chromatic, sharpen, vignette, grain, screen;
    float exposure, contrast = 1, saturation = 1, gamma = 1, temperature, tintShift, hue, vibrance;
    int tintColor = 0xFFFFFFFF;
    float tintStrength;
    float vigIntensity, vigRadius = 1, vigSoftness = 0.5f, vigRoundness = 1;
    int vigColor = 0xFF000000;
    float caStrength, caRadial;
    float grainIntensity, grainSize = 1, grainAnimated, grainResponse;
    float sharpenAmount, sharpenRadius = 1;
    float scanlines, scanDensity = 2, curvature, pixelate, rgbMask, flicker;

    boolean any() { return grading || tint || chromatic || sharpen || vignette || grain || screen; }

    static UberParams collect() {
        UberParams p = new UberParams();
        ColorGrading g = ColorGrading.INSTANCE;
        if (g != null && g.isEnabled()) {
            p.grading = true;
            p.exposure = (float) g.exposure.get().doubleValue();
            p.contrast = g.contrast.getFloat();
            p.saturation = g.saturation.getFloat();
            p.gamma = g.gamma.getFloat();
            p.temperature = g.temperature.getFloat();
            p.tintShift = g.tint.getFloat();
            p.hue = g.hueShift.getFloat() / 360f;
            p.vibrance = g.vibrance.getFloat();
        }
        ScreenTint t = ScreenTint.INSTANCE;
        if (t != null && t.isEnabled()) {
            p.tint = true;
            p.tintColor = t.color.getColor();
            p.tintStrength = t.strength.getFloat();
        }
        ChromaticAberration ca = ChromaticAberration.INSTANCE;
        if (ca != null && ca.isEnabled()) {
            p.chromatic = true;
            p.caStrength = ca.currentStrength();
            p.caRadial = ca.mode.is("Radial") ? 1 : 0;
        }
        Sharpening sh = Sharpening.INSTANCE;
        if (sh != null && sh.isEnabled()) {
            p.sharpen = true;
            p.sharpenAmount = sh.amount.getFloat();
            p.sharpenRadius = sh.radius.getFloat();
        }
        Vignette v = Vignette.INSTANCE;
        if (v != null && v.isEnabled()) {
            p.vignette = true;
            p.vigIntensity = v.intensity.getFloat();
            p.vigRadius = v.radius.getFloat();
            p.vigSoftness = v.softness.getFloat();
            p.vigRoundness = v.roundness.getFloat();
            p.vigColor = v.color.getColor();
        }
        ZoomVisuals zv = ZoomVisuals.INSTANCE;
        if (zv != null && zv.isEnabled() && zv.progress() > 0.01f && zv.vignette.get()) {
            float z = zv.progress();
            p.vignette = true;
            p.vigIntensity = Math.max(p.vigIntensity, zv.vignetteStrength.getFloat() * z);
            p.vigRadius = p.vignette && v != null && v.isEnabled() ? Math.min(p.vigRadius, 1f - 0.35f * z) : 1f - 0.35f * z;
            p.vigSoftness = 0.45f;
            p.vigRoundness = 1f;
            if (!(v != null && v.isEnabled())) p.vigColor = 0xFF000000;
        }
        FilmGrain fg = FilmGrain.INSTANCE;
        if (fg != null && fg.isEnabled()) {
            p.grain = true;
            p.grainIntensity = fg.intensity.getFloat();
            p.grainSize = fg.size.getFloat();
            p.grainAnimated = fg.animated.get() ? 1 : 0;
            p.grainResponse = fg.luminanceResponse.getFloat();
        }
        ScreenEffects se = ScreenEffects.INSTANCE;
        if (se != null && se.isEnabled()) {
            p.screen = true;
            p.scanlines = se.scanlines.getFloat();
            p.scanDensity = se.scanlineSize.getFloat();
            p.curvature = se.curvature.getFloat();
            p.pixelate = se.pixelate.getFloat();
            p.rgbMask = se.rgbMask.getFloat();
            p.flicker = se.flicker.getFloat();
        }
        return p;
    }

    private static float r(int c) { return ColorUtil.red(c) / 255f; }
    private static float g(int c) { return ColorUtil.green(c) / 255f; }
    private static float b(int c) { return ColorUtil.blue(c) / 255f; }
    private static float f(boolean v) { return v ? 1f : 0f; }

    void write(Std140Builder s, int w, int h, float time) {
        s.putVec4(exposure, contrast, saturation, gamma)
                .putVec4(temperature, tintShift, hue, vibrance)
                .putVec4(r(tintColor), g(tintColor), b(tintColor), tintStrength)
                .putVec4(vigIntensity, vigRadius, vigSoftness, vigRoundness)
                .putVec4(r(vigColor), g(vigColor), b(vigColor), 0)
                .putVec4(caStrength, caRadial, 0, 0)
                .putVec4(grainIntensity, grainSize, grainAnimated, grainResponse)
                .putVec4(sharpenAmount, sharpenRadius, 0, 0)
                .putVec4(scanlines, scanDensity, curvature, pixelate)
                .putVec4(rgbMask, flicker, 0, 0)
                .putVec4(f(grading), f(tint), f(chromatic), f(sharpen))
                .putVec4(f(vignette), f(grain), f(screen), 0)
                .putVec4(w, h, time, 0);
    }
}
