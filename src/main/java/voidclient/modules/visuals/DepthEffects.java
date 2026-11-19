package voidclient.modules.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.utilities.MathUtil;

/** Depth-buffer effects: distance fog tint and depth of field (hand stays sharp). */
public class DepthEffects extends Module {
    public static DepthEffects INSTANCE;

    public final BooleanSetting fog = add(new BooleanSetting("Depth Fog", "Tint distant geometry by linear depth.", true));
    public final NumberSetting fogStart = add(new NumberSetting("Fog Start", "Distance where depth fog begins.", 24, 0, 256, 1).unit("m").visibleWhen(fog::get));
    public final NumberSetting fogEnd = add(new NumberSetting("Fog End", "Distance of full fog.", 160, 8, 512, 1).unit("m").visibleWhen(fog::get));
    public final NumberSetting fogStrength = add(new NumberSetting("Fog Strength", "Maximum fog opacity.", 0.45, 0, 1, 0.01).visibleWhen(fog::get));
    public final ColorSetting fogColor = add(new ColorSetting("Fog Color", "Depth fog colour.", 0xFF3A1F66, false).visibleWhen(fog::get));
    public final BooleanSetting dof = add(new BooleanSetting("Depth Of Field", "Blur geometry outside the focus range.", false));
    public final ModeSetting focusMode = add(new ModeSetting("Focus Mode", "Auto focuses on what the crosshair hits.", "Auto", "Auto", "Manual").visibleWhen(dof::get));
    public final NumberSetting focusDistance = add(new NumberSetting("Focus Distance", "Manual focus distance.", 12, 0.5, 128, 0.5).unit("m").visibleWhen(() -> dof.get() && focusMode.is("Manual")));
    public final NumberSetting focusRange = add(new NumberSetting("Focus Range", "Depth range kept sharp.", 6, 0.5, 64, 0.5).unit("m").visibleWhen(dof::get));
    public final NumberSetting maxBlur = add(new NumberSetting("Max Blur", "Largest blur radius.", 6, 1, 16, 0.5).unit("px").visibleWhen(dof::get));
    private double autoFocus = 10;

    public DepthEffects() {
        super("Depth Effects", "Depth-buffer fog tint and depth of field.", Category.VISUALS);
        INSTANCE = this;
    }

    public boolean fogEnabled() { return fog.get(); }
    public boolean dofEnabled() { return dof.get(); }
    public double fogStart() { return fogStart.get(); }
    public double fogEnd() { return Math.max(fogStart.get() + 1, fogEnd.get()); }
    public double fogStrength() { return fogStrength.get(); }
    public int fogColor() { return fogColor.getColor(); }
    public double focusRange() { return focusRange.get(); }
    public double maxBlur() { return maxBlur.get(); }

    public double focusDistance() {
        if (focusMode.is("Manual")) return focusDistance.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        double target = 32;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS && mc.player != null) {
            target = mc.crosshairTarget.getPos().distanceTo(mc.gameRenderer.getCamera().getCameraPos());
        }
        autoFocus = MathUtil.lerp(autoFocus, target, 0.15);
        return autoFocus;
    }
}
