package voidclient.modules.visuals;

import org.lwjgl.glfw.GLFW;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.keybinds.Keybind;
import voidclient.utilities.MathUtil;

/** Smooth hold-to-zoom with scroll adjustment and sensitivity scaling. */
public class Zoom extends Module {
    public static Zoom INSTANCE;

    public final NumberSetting factor = add(new NumberSetting("Zoom Factor", "Magnification while zooming.", 4, 1.5, 50, 0.5).unit("x"));
    public final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Animate the zoom in and out.", true));
    public final NumberSetting smoothSpeed = add(new NumberSetting("Smooth Speed", "Zoom animation speed.", 12, 2, 40, 1).visibleWhen(smooth::get));
    public final BooleanSetting scroll = add(new BooleanSetting("Scroll Adjust", "Mouse wheel changes the zoom while zooming.", true));
    public final BooleanSetting scaleSensitivity = add(new BooleanSetting("Scale Sensitivity", "Lower mouse sensitivity proportionally to the zoom.", true));
    public final BooleanSetting cinematic = add(new BooleanSetting("Cinematic Camera", "Use the smooth cinematic camera while zooming.", false));
    private float progress;
    private float scrollFactor = 1f;
    private long lastNanos = System.nanoTime();
    private boolean prevSmoothCamera;

    public Zoom() {
        super("Zoom", "Hold the keybind to zoom smoothly; scroll to adjust.", Category.VISUALS);
        INSTANCE = this;
        keybind.defaultTo(new Keybind(GLFW.GLFW_KEY_C, false, true));
        toggleNotification.defaultTo(false);
        showInModuleList.defaultTo(false);
        on(Events.MouseScroll.class, e -> {
            if (!scroll.get()) return;
            scrollFactor = (float) MathUtil.clamp(scrollFactor * (e.vertical > 0 ? 1.15 : 1 / 1.15), 0.25, 8);
            e.cancel();
        });
    }

    @Override
    protected void onEnable() {
        scrollFactor = 1f;
        if (cinematic.get() && mc.options != null) {
            prevSmoothCamera = mc.options.smoothCameraEnabled;
            mc.options.smoothCameraEnabled = true;
        }
    }

    @Override
    protected void onDisable() {
        if (cinematic.get() && mc.options != null) mc.options.smoothCameraEnabled = prevSmoothCamera;
    }

    public float currentZoom() {
        return 1f + (Math.max(1.01f, factor.getFloat() * scrollFactor) - 1f) * progress;
    }

    public float progress() { return progress; }

    public float apply(float fov) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        float target = isEnabled() ? 1f : 0f;
        progress = smooth.get() ? (float) MathUtil.approach(progress, target, smoothSpeed.get(), dt) : target;
        if (Math.abs(progress - target) < 0.001f) progress = target;
        return progress <= 0f ? fov : fov / currentZoom();
    }

    public float lookScale() {
        return scaleSensitivity.get() && progress > 0 ? 1f / currentZoom() : 1f;
    }

    @Override
    public String getInfo() { return String.format(java.util.Locale.ROOT, "%.1fx", factor.get() * scrollFactor); }
}
