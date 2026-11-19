package voidclient.modules.basehunting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.gui.themes.ThemeManager;
import voidclient.keybinds.Keybind;
import voidclient.mixin.InputAccessor;
import voidclient.render.FontRenderer;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/**
 * Client-side detached camera. The real player receives no movement or look input, so its
 * server-side position never changes; only chunks/entities the client already has are visible.
 */
public class Freecam extends Module {
    public static Freecam INSTANCE;

    public record CameraState(double x, double y, double z, float yaw, float pitch) {}

    public final ModeSetting allowed = add(new ModeSetting("Allowed In", "Where Freecam may be enabled.", "Singleplayer + Spectator", "Singleplayer + Spectator", "Anywhere"));
    public final ModeSetting mode = add(new ModeSetting("Camera Mode", "Free flight, cinematic smoothing, or orbit around your player.", "Free", "Free", "Cinematic", "Orbit"));
    public final NumberSetting speed = add(new NumberSetting("Camera Speed", "Base flight speed (blocks/tick).", 0.6, 0.05, 5, 0.05));
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal Speed", "Horizontal speed multiplier.", 1, 0.1, 4, 0.05).unit("x"));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed", "Vertical speed multiplier.", 0.8, 0.1, 4, 0.05).unit("x"));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", "Motion inertia (0 = instant).", 0.4, 0, 0.95, 0.05));
    public final NumberSetting fov = add(new NumberSetting("FOV", "Camera FOV while in freecam (0 = unchanged).", 0, 0, 130, 1).unit("°"));
    public final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "Mouse look multiplier.", 1, 0.1, 3, 0.05).unit("x"));
    public final NumberSetting orbitRadius = add(new NumberSetting("Orbit Radius", "Distance from the player in Orbit mode.", 6, 2, 32, 0.5).visibleWhen(() -> mode.is("Orbit")));
    public final KeybindSetting resetKey = add(new KeybindSetting("Reset Camera", "Return the camera to your eyes.", Keybind.key(org.lwjgl.glfw.GLFW.GLFW_KEY_HOME)));
    public final BooleanSetting allowInteract = add(new BooleanSetting("Allow Interaction", "Let clicks interact from your real position.", false));
    public final BooleanSetting hud = add(new BooleanSetting("Cinematic HUD", "Show the camera HUD (position, speed, REC).", true));
    public final BooleanSetting letterbox = add(new BooleanSetting("Letterbox", "Cinema bars.", false));
    public final BooleanSetting hideClientHud = add(new BooleanSetting("Hide Client HUD", "Hide Void HUD elements while the camera is detached.", true));

    private double x, y, z, px, py, pz, vx, vy, vz;
    private float yaw, pitch, pyaw, ppitch, targetYaw, targetPitch;
    private PlayerInput input = PlayerInput.DEFAULT;
    private long started;

    public Freecam() {
        super("Freecam", "Detached client-side camera for inspecting your surroundings.", Category.BASE_HUNTING);
        INSTANCE = this;
        on(Events.TickPre.class, e -> tick());
        on(Events.Key.class, e -> {
            if (e.action == 1 && mc.currentScreen == null && resetKey.get().matchesKey(e.key)) resetToPlayer();
        });
        on(Events.HudRender.class, this::drawHud);
        on(Events.WorldLeave.class, e -> setEnabled(false));
    }

    private boolean permitted() {
        if (allowed.is("Anywhere")) return true;
        return mc.isInSingleplayer() || (mc.player != null && mc.player.isSpectator());
    }

    @Override
    protected void onEnable() {
        if (!inWorld()) {
            setEnabled(false);
            return;
        }
        if (!permitted()) {
            Notifications.warning("Freecam", "Only allowed in singleplayer or spectator (see Allowed In)");
            setEnabled(false);
            return;
        }
        resetToPlayer();
        started = System.currentTimeMillis();
    }

    @Override
    protected void onDisable() {
        vx = vy = vz = 0;
    }

    private void resetToPlayer() {
        if (mc.player == null) return;
        Vec3d eye = mc.player.getEyePos();
        x = px = eye.x;
        y = py = eye.y;
        z = pz = eye.z;
        yaw = pyaw = targetYaw = mc.player.getYaw();
        pitch = ppitch = targetPitch = mc.player.getPitch();
        vx = vy = vz = 0;
    }

    private void tick() {
        if (mc.player == null) return;
        if (!permitted()) {
            setEnabled(false);
            return;
        }
        px = x;
        py = y;
        pz = z;
        pyaw = yaw;
        ppitch = pitch;
        boolean cinematic = mode.is("Cinematic");
        if (cinematic) {
            yaw += (targetYaw - yaw) * 0.25f;
            pitch += (targetPitch - pitch) * 0.25f;
        } else {
            yaw = targetYaw;
            pitch = targetPitch;
        }
        if (mode.is("Orbit")) {
            Vec3d c = mc.player.getEyePos();
            double r = orbitRadius.get();
            double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
            x = c.x + Math.sin(ry) * Math.cos(rp) * r;
            y = c.y + Math.sin(rp) * r;
            z = c.z - Math.cos(ry) * Math.cos(rp) * r;
            return;
        }
        double f = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double s = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        double u = (input.jump() ? 1 : 0) - (input.sneak() ? 1 : 0);
        double sp = speed.get() * (input.sprint() ? 2 : 1);
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double tx = (-sin * f + cos * s) * sp * horizontal.get();
        double tz = (cos * f + sin * s) * sp * horizontal.get();
        double ty = u * sp * vertical.get();
        double k = cinematic ? Math.max(0.85, smoothness.get()) : smoothness.get();
        vx = vx * k + tx * (1 - k);
        vy = vy * k + ty * (1 - k);
        vz = vz * k + tz * (1 - k);
        x += vx;
        y += vy;
        z += vz;
    }

    // ---- mixin hooks ---------------------------------------------------------------------------

    public static boolean blocksInteraction() {
        Freecam f = INSTANCE;
        return f != null && f.isEnabled() && !f.allowInteract.get();
    }

    /** Consumes mouse look while active. */
    public static boolean onLook(double dx, double dy) {
        Freecam f = INSTANCE;
        if (f == null || !f.isEnabled()) return false;
        double s = f.sensitivity.get() * 0.15;
        f.targetYaw += (float) (dx * s);
        f.targetPitch = MathHelper.clamp(f.targetPitch + (float) (dy * s), -90f, 90f);
        return true;
    }

    /** Copies movement keys into the camera and zeroes the player's input. */
    public static void captureInput(KeyboardInput input) {
        Freecam f = INSTANCE;
        if (f == null || !f.isEnabled() || MinecraftClient.getInstance().player == null) return;
        f.input = input.playerInput;
        input.playerInput = PlayerInput.DEFAULT;
        ((InputAccessor) input).voidclient$setMovementVector(Vec2f.ZERO);
    }

    public static boolean hidesClientHud() {
        Freecam f = INSTANCE;
        return f != null && f.isEnabled() && f.hideClientHud.get();
    }

    /** The camera starts inside your head; skip drawing your own model until it has moved away. */
    public static boolean hidesEntity(net.minecraft.entity.Entity e) {
        Freecam f = INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (f == null || !f.isEnabled() || e != mc.player) return false;
        return e.getEyePos().squaredDistanceTo(f.x, f.y, f.z) < 1.2 * 1.2;
    }

    public static CameraState cameraState(float tickDelta) {
        Freecam f = INSTANCE;
        if (f == null || !f.isEnabled()) return null;
        return new CameraState(MathHelper.lerp(tickDelta, f.px, f.x), MathHelper.lerp(tickDelta, f.py, f.y), MathHelper.lerp(tickDelta, f.pz, f.z),
                MathHelper.lerpAngleDegrees(tickDelta, f.pyaw, f.yaw), MathHelper.lerp(tickDelta, f.ppitch, f.pitch));
    }

    // ---- cinematic HUD ---------------------------------------------------------------------------

    private void drawHud(Events.HudRender e) {
        DrawContext ctx = e.ctx;
        if (letterbox.get()) {
            float bar = e.height * 0.1f;
            Render2D.rect(ctx, 0, 0, e.width, bar, 0xFF000000);
            Render2D.rect(ctx, 0, e.height - bar, e.width, bar, 0xFF000000);
        }
        if (!hud.get()) return;
        double speedBps = Math.sqrt(vx * vx + vy * vy + vz * vz) * 20;
        long t = (System.currentTimeMillis() - started) / 1000;
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        // Bottom centre (inside the lower cinema bar when letterboxed) so it never covers HUD elements.
        String line1 = String.format(Locale.ROOT, "REC %02d:%02d  FREECAM  %s", t / 60, t % 60, mode.get().toUpperCase(Locale.ROOT));
        String line2 = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f   YAW %.0f  PITCH %.0f   %.1f b/s", x, y, z, MathHelper.wrapDegrees(yaw), pitch, speedBps);
        float y0 = letterbox.get() ? e.height - e.height * 0.1f + 4 : e.height - 72;
        float w1 = FontRenderer.width(line1, FontRenderer.Family.MONO) + 10, w2 = FontRenderer.width(line2, FontRenderer.Family.MONO);
        float x1 = (e.width - w1) / 2f, x2 = (e.width - w2) / 2f;
        if (blink) Render2D.circle(ctx, x1 + 3, y0 + 4, 3, 0xFFFF3040);
        FontRenderer.draw(ctx, line1, x1 + 10, y0, 0xFFFFFFFF, FontRenderer.Family.MONO, true);
        FontRenderer.draw(ctx, line2, x2, y0 + 11, ColorUtil.withAlpha(ThemeManager.accent(0), 230), FontRenderer.Family.MONO, true);
        float cx = e.width / 2f, cy = e.height / 2f;
        int c = 0x90FFFFFF;
        Render2D.rect(ctx, cx - 30, cy - 18, 8, 1, c);
        Render2D.rect(ctx, cx - 30, cy - 18, 1, 8, c);
        Render2D.rect(ctx, cx + 22, cy - 18, 8, 1, c);
        Render2D.rect(ctx, cx + 29, cy - 18, 1, 8, c);
        Render2D.rect(ctx, cx - 30, cy + 17, 8, 1, c);
        Render2D.rect(ctx, cx - 30, cy + 10, 1, 8, c);
        Render2D.rect(ctx, cx + 22, cy + 17, 8, 1, c);
        Render2D.rect(ctx, cx + 29, cy + 10, 1, 8, c);
    }

    @Override
    public String getInfo() { return mode.get(); }
}
