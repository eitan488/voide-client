package voidclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voidclient.config.ClientSettings;
import voidclient.config.ClientState;
import voidclient.config.ConfigManager;
import voidclient.config.ProfileManager;
import voidclient.core.ModuleManager;
import voidclient.core.ModuleRegistry;
import voidclient.events.EventBus;
import voidclient.events.Events;
import voidclient.gui.screens.ClickGuiScreen;
import voidclient.hud.HudManager;
import voidclient.keybinds.KeybindManager;
import voidclient.modules.utility.NotificationSystem;
import voidclient.render.RenderManager;
import voidclient.utilities.CombatTracker;
import voidclient.utilities.SessionTracker;

/** Void Client entrypoint: builds every manager, registers modules and wires Fabric events. */
public final class VoidClient implements ClientModInitializer {
    public static final String MOD_ID = "voidclient";
    public static final String NAME = "Void Client";
    public static final String VERSION = "1.0.0";
    public static final String MC_VERSION = "1.21.11";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static VoidClient instance;

    private final EventBus events = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final ConfigManager config = new ConfigManager();
    private final ProfileManager profiles = new ProfileManager(config);
    private final KeybindManager keybinds = new KeybindManager();
    private final ClientSettings clientSettings = new ClientSettings();
    private final ClientState clientState = new ClientState();
    private final HudManager hud = new HudManager();
    private final CombatTracker combat = new CombatTracker();
    private final SessionTracker session = new SessionTracker();
    private KeyBinding openGuiKey;
    private final Events.HudRender hudEvent = new Events.HudRender();
    private final Events.WorldRender worldEvent = new Events.WorldRender();

    public static VoidClient get() { return instance; }

    public EventBus events() { return events; }
    public ModuleManager modules() { return modules; }
    public ConfigManager config() { return config; }
    public ProfileManager profiles() { return profiles; }
    public KeybindManager keybinds() { return keybinds; }
    public ClientSettings clientSettings() { return clientSettings; }
    public ClientState clientState() { return clientState; }
    public HudManager hud() { return hud; }
    public CombatTracker combat() { return combat; }
    public SessionTracker session() { return session; }
    public KeyBinding openGuiKey() { return openGuiKey; }

    public NotificationSystem notificationSettings() {
        return modules.all().isEmpty() ? null : modules.get(NotificationSystem.class);
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        long start = System.currentTimeMillis();

        ModuleRegistry.registerAll(modules);
        keybinds.init();
        combat.init();
        session.init();
        config.loadClient();
        profiles.init();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.voidclient.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.START_CLIENT_TICK.register(mc -> events.post(Events.TickPre.INSTANCE));
        ClientTickEvents.END_CLIENT_TICK.register(this::endTick);

        // TAIL of InGameHud.render: runs even when F1 hides the vanilla HUD (letterbox, cinematic HUD)
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            hudEvent.hudHidden = mc.options.hudHidden;
            hudEvent.ctx = ctx;
            hudEvent.tickDelta = tickCounter.getTickProgress(true);
            hudEvent.width = ctx.getScaledWindowWidth();
            hudEvent.height = ctx.getScaledWindowHeight();
            if (mc.player != null) {
                events.post(hudEvent);
                hud.render(ctx, hudEvent.tickDelta);
            }
        });

        WorldRenderEvents.END_MAIN.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            worldEvent.context = context;
            worldEvent.matrices = context.matrices();
            worldEvent.consumers = context.consumers();
            worldEvent.cameraPos = context.worldState().cameraRenderState.pos;
            worldEvent.tickDelta = mc.getRenderTickCounter().getTickProgress(true);
            RenderManager.begin(worldEvent);
            events.post(worldEvent);
            RenderManager.end(worldEvent);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            String key = mc.isInSingleplayer() && mc.getServer() != null
                    ? "sp_" + mc.getServer().getSaveProperties().getLevelName()
                    : mc.getCurrentServerEntry() != null ? "mp_" + mc.getCurrentServerEntry().address : "unknown";
            events.post(new Events.WorldJoin(key.replaceAll("[^A-Za-z0-9_.\\-]", "_")));
        });
        // DISCONNECT can arrive on a network thread; world-leave handlers free GPU resources.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            Runnable leave = () -> {
                events.post(new Events.WorldLeave());
                config.saveAll();
            };
            if (mc.isOnThread()) leave.run();
            else mc.execute(leave);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) events.post(new Events.AttackEntity(entity));
            return ActionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) events.post(new Events.AttackBlock(pos, direction));
            return ActionResult.PASS;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> config.saveAll());
        voidclient.utilities.ClientCommands.register();
        if (voidclient.dev.SelfTest.active()) voidclient.dev.SelfTest.start();

        LOG.info("{} {} initialised {} modules in {} ms", NAME, VERSION, modules.all().size(), System.currentTimeMillis() - start);
    }

    private void endTick(MinecraftClient mc) {
        events.post(Events.TickPost.INSTANCE);
        while (openGuiKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen(null));
        }
        config.tick();
    }
}
