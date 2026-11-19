package voidclient.dev;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.MixinEnvironment;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.events.Events;
import voidclient.gui.screens.ChunkBrowserScreen;
import voidclient.gui.screens.ClickGuiScreen;
import voidclient.gui.screens.HudEditorScreen;
import voidclient.gui.screens.MainMenuScreen;
import voidclient.gui.screens.ServerBrowserScreen;
import voidclient.gui.screens.WorldMapScreen;
import voidclient.modules.basehunting.ChunkIntelligence;
import voidclient.modules.crystal.CrystalTrainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Development smoke test, only active with -Dvoidclient.selftest=true (gradle runSelftest).
 * Walks the real client through menus, a new world and the default profiles, logging
 * "SHOT name" markers for an external screen grabber, then writes run/voidclient-selftest.txt.
 */
public final class SelfTest {
    private record Step(String name, BooleanSupplier ready, Runnable action, int waitAfter, String shot) {}

    private static final List<String> LOG = new ArrayList<>();
    private static final List<Step> STEPS = new ArrayList<>();
    private static int index, wait;
    private static boolean worldRequested;

    private SelfTest() {}

    public static boolean active() { return Boolean.getBoolean("voidclient.selftest"); }

    private static void log(String s) {
        LOG.add(s);
        VoidClient.LOG.info("[SelfTest] {}", s);
    }

    private static void step(String name, BooleanSupplier ready, Runnable action, int waitAfter, String shot) {
        STEPS.add(new Step(name, ready, action, waitAfter, shot));
    }

    private static void step(String name, Runnable action, int waitAfter, String shot) { step(name, () -> true, action, waitAfter, shot); }

    public static void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        step("reach main menu", () -> {
            if (mc.getOverlay() == null && mc.currentScreen instanceof AccessibilityOnboardingScreen) {
                mc.options.onboardAccessibility = false;
                mc.options.write();
                mc.setScreen(new TitleScreen());
                return false;
            }
            return mc.getOverlay() == null && mc.currentScreen instanceof MainMenuScreen;
        }, () -> {
            MixinEnvironment.getCurrentEnvironment().audit();
            log("Mixin audit passed (all mixins applied)");
        }, 80, "01-main-menu");
        // VOIDCLIENT_SELFTEST_QUICK=1: a short pass (menu, GUI, world, HUD editor) for extra resolutions.
        boolean quick = "1".equals(System.getenv("VOIDCLIENT_SELFTEST_QUICK"));
        Category[] pages = quick ? new Category[]{Category.DASHBOARD, Category.SETTINGS}
                : new Category[]{Category.DASHBOARD, Category.COMBAT, Category.CRYSTAL, Category.BASE_HUNTING, Category.VISUALS,
                Category.HUD_EDITOR, Category.PROFILES, Category.SETTINGS};
        for (Category c : pages) {
            step("gui " + c, () -> mc.setScreen(new ClickGuiScreen(new MainMenuScreen(), c)), 60, "02-gui-" + c.name().toLowerCase());
        }
        step("gui module selected", () -> {
            ClickGuiScreen s = new ClickGuiScreen(new MainMenuScreen(), Category.VISUALS);
            mc.setScreen(s);
            s.openModule(VoidClient.get().modules().byName("Bloom").orElseThrow());
        }, 60, "02-gui-bloom-settings");
        if (!quick) step("server browser", () -> mc.setScreen(new ServerBrowserScreen(new MainMenuScreen())), 60, "03-server-browser");
        step("create world", () -> true, SelfTest::createWorld, 40, null);
        step("world loaded", () -> mc.player != null && mc.world != null && mc.currentScreen == null && !worldPending(), () -> log("World loaded"), 300, "04-ingame-default-profile");
        step("layout Default", () -> reportOverlaps("Default"), 0, null);
        for (String p : quick ? new String[]{"Performance", "Crystal Practice"} : new String[]{"PvP Practice", "Visual Showcase", "Base Inspection", "Anchor Practice", "Performance", "Crystal Practice"}) {
            step("profile " + p, () -> VoidClient.get().profiles().switchTo(p), 160, "05-profile-" + p.toLowerCase().replace(' ', '-'));
            step("layout " + p, () -> reportOverlaps(p), 0, null);
        }
        step("hud editor", () -> mc.setScreen(new HudEditorScreen(null)), 60, "07-hud-editor");
        if (!quick) {
            step("crystal trainer", () -> VoidClient.get().modules().get(CrystalTrainer.class).setEnabled(true), 200, "06-crystal-trainer");
            step("stop trainer", () -> VoidClient.get().modules().get(CrystalTrainer.class).setEnabled(false), 20, null);
            step("world map", () -> mc.setScreen(new WorldMapScreen(null)), 80, "08-world-map");
            step("chunk browser", () -> {
                log("Chunk Intelligence scanned chunks: " + VoidClient.get().modules().get(ChunkIntelligence.class).scanner().results().size());
                mc.setScreen(new ChunkBrowserScreen(null));
            }, 60, "09-chunk-browser");
            step("ingame gui", () -> mc.setScreen(new ClickGuiScreen(null, Category.SETTINGS)), 60, "10-gui-ingame-settings");
            step("freecam", () -> {
                mc.setScreen(null);
                VoidClient.get().modules().byName("Freecam").orElseThrow().setEnabled(true);
            }, 80, "11-freecam");
            step("all modules", () -> {
                VoidClient.get().modules().byName("Freecam").orElseThrow().setEnabled(false);
                int n = 0;
                for (Module m : VoidClient.get().modules().all()) {
                    if (!m.isToggleable() || List.of("Freecam", "Screenshot Mode", "Custom Glass", "Zoom", "Inventory Manager").contains(m.getName()) || m.getName().contains("Trainer")) continue;
                    m.setEnabled(true);
                    n++;
                }
                log("Enabled " + n + " modules at once");
            }, 200, "12-stress-all-modules");
        }
        step("report", () -> {
            if (!quick) for (Module m : VoidClient.get().modules().all()) {
                if (m.isToggleable() && !m.isEnabled() && !List.of("Freecam", "Screenshot Mode", "Custom Glass", "Zoom", "Inventory Manager").contains(m.getName())
                        && !m.getName().contains("Trainer")) log("DISABLED BY ERROR HANDLER: " + m.getName());
            }
            log("Post-processing failed flag: " + voidclient.shaders.PostProcessingManager.get().hasFailed());
            finish(mc);
        }, 0, null);
        log("Self test armed with " + STEPS.size() + " steps");
        VoidClient.get().events().subscribe(Events.TickPost.class, e -> tick());
    }

    /** Logs every pair of visible HUD elements whose on-screen rectangles intersect. */
    private static void reportOverlaps(String profile) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        List<voidclient.core.HudModule> shown = new java.util.ArrayList<>();
        for (Module m : VoidClient.get().modules().all()) {
            if (m instanceof voidclient.core.HudModule h && h.isEnabled() && h.shouldRender() && h.getWidth() > 4.5f && h.getHeight() > 4.5f) shown.add(h);
        }
        int overlaps = 0;
        for (int i = 0; i < shown.size(); i++) {
            for (int j = i + 1; j < shown.size(); j++) {
                var a = shown.get(i);
                var b = shown.get(j);
                float ax = a.screenX(sw), ay = a.screenY(sh), aw = a.getWidth() * a.getScale(), ah = a.getHeight() * a.getScale();
                float bx = b.screenX(sw), by = b.screenY(sh), bw = b.getWidth() * b.getScale(), bh = b.getHeight() * b.getScale();
                float ix = Math.min(ax + aw, bx + bw) - Math.max(ax, bx), iy = Math.min(ay + ah, by + bh) - Math.max(ay, by);
                if (ix > 1 && iy > 1) {
                    overlaps++;
                    log("HUD OVERLAP [" + profile + "] " + a.getName() + " <-> " + b.getName() + String.format(java.util.Locale.ROOT, " (%.0fx%.0f px)", ix, iy));
                }
            }
        }
        log("HUD layout [" + profile + "]: " + shown.size() + " visible elements, " + overlaps + " overlaps (screen " + sw + "x" + sh + ")");
    }

    private static boolean pendingWorld;

    private static boolean worldPending() { return pendingWorld && MinecraftClient.getInstance().player == null; }

    private static void createWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!worldRequested) {
            worldRequested = true;
            pendingWorld = true;
            CreateWorldScreen.show(mc, () -> mc.setScreen(new MainMenuScreen()));
        }
    }

    private static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (index >= STEPS.size()) return;
        if (wait > 0) {
            wait--;
            return;
        }
        Step s = STEPS.get(index);
        try {
            if (s.name().equals("world loaded") && mc.currentScreen instanceof CreateWorldScreen cws) {
                for (var child : cws.children()) {
                    if (child instanceof ButtonWidget b && b.getMessage().getString().equals(Text.translatable("selectWorld.create").getString())) {
                        log("Pressing Create New World");
                        b.onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
                        wait = 40;
                        return;
                    }
                }
            }
            if (!s.ready().getAsBoolean()) {
                wait = 10;
                return;
            }
            if (s.shot() != null && s.action() != null && pendingShot == null) {
                s.action().run();
                pendingShot = s.shot();
                wait = s.waitAfter();
                return;
            }
            if (pendingShot != null) {
                log("SHOT " + pendingShot + " (screen=" + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName()) + ")");
                pendingShot = null;
                wait = 80;
                index++;
                return;
            }
            s.action().run();
            wait = s.waitAfter();
            index++;
        } catch (Throwable t) {
            log("FAILED at step '" + s.name() + "': " + t);
            VoidClient.LOG.error("[SelfTest] failure", t);
            finish(mc);
        }
    }

    private static String pendingShot;

    private static void finish(MinecraftClient mc) {
        log("Self test finished");
        try {
            Files.writeString(mc.runDirectory.toPath().resolve("voidclient-selftest.txt"), String.join(System.lineSeparator(), LOG), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        index = STEPS.size();
        mc.scheduleStop();
    }
}
