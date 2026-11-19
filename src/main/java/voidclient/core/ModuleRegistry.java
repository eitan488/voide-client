package voidclient.core;

import voidclient.modules.anchor.AnchorHud;
import voidclient.modules.anchor.AnchorTrainer;
import voidclient.modules.anchor.AnchorVisualizer;
import voidclient.modules.basehunting.BlockEsp;
import voidclient.modules.basehunting.ChunkBrowser;
import voidclient.modules.basehunting.ChunkInspectionEsp;
import voidclient.modules.basehunting.ChunkIntelligence;
import voidclient.modules.basehunting.CustomEntityEsp;
import voidclient.modules.basehunting.EntityEsp;
import voidclient.modules.basehunting.Freecam;
import voidclient.modules.basehunting.FriendEsp;
import voidclient.modules.basehunting.InspectionHistory;
import voidclient.modules.basehunting.InspectionProfiles;
import voidclient.modules.basehunting.ItemEsp;
import voidclient.modules.basehunting.PlayerEsp;
import voidclient.modules.basehunting.StorageEsp;
import voidclient.modules.basehunting.WaypointEsp;
import voidclient.modules.combat.*;
import voidclient.modules.crystal.CrystalHud;
import voidclient.modules.crystal.CrystalInfo;
import voidclient.modules.crystal.CrystalTrainer;
import voidclient.modules.crystal.CrystalVisuals;
import voidclient.modules.misc.*;
import voidclient.modules.movement.*;
import voidclient.modules.player.*;
import voidclient.modules.render.*;
import voidclient.modules.utility.*;
import voidclient.modules.visuals.*;
import voidclient.modules.world.*;
import voidclient.particles.ParticleManager;
import voidclient.utilities.ChunkTracker;

/** Registration order = display order in the GUI (follows the specification lists). */
public final class ModuleRegistry {
    private ModuleRegistry() {}

    public static void registerAll(ModuleManager m) {
        ChunkTracker.init();
        ParticleManager.get().init();

        // Combat (original 1-8, then combat utilities)
        m.register(new ArmorHud());
        m.register(new CombatCrosshair());
        m.register(new HitColor());
        m.register(new DamageIndicator());
        m.register(new TargetHud());
        m.register(new CombatInfo());
        m.register(new TotemCounter());
        m.register(new ShieldInfo());
        m.register(new CombatHud());
        m.register(new ArmorDurabilityWarning());
        m.register(new PotionTimer());
        m.register(new CombatSessionTimer());
        m.register(new AttackStatistics());
        m.register(new HotbarDisplay());
        m.register(new ItemInformation());
        m.register(new CooldownInformation());
        m.register(new TrainingNotifications());

        // Crystal PvP
        m.register(new CrystalHud());
        m.register(new CrystalInfo());
        m.register(new CrystalTrainer());
        m.register(new CrystalVisuals());

        // Anchor PvP
        m.register(new AnchorHud());
        m.register(new AnchorTrainer());
        m.register(new AnchorVisualizer());

        // Base hunting
        m.register(new ChunkIntelligence());
        m.register(new ChunkInspectionEsp());
        m.register(new InspectionProfiles());
        m.register(new ChunkBrowser());
        m.register(new InspectionHistory());
        m.register(new Freecam());
        m.register(new PlayerEsp());
        m.register(new EntityEsp());
        m.register(new ItemEsp());
        m.register(new StorageEsp());
        m.register(new BlockEsp());
        m.register(new WaypointEsp());
        m.register(new FriendEsp());
        m.register(new CustomEntityEsp());

        // Movement
        m.register(new Sprint());
        m.register(new ToggleSneak());
        m.register(new SafeWalk());
        m.register(new MovementHud());
        m.register(new Coordinates());
        m.register(new TimerDisplay());
        m.register(new StepStatistics());
        m.register(new ElytraHud());
        m.register(new HorseHud());

        // Player
        m.register(new AutoTool());
        m.register(new InventoryManager());
        m.register(new InventoryHud());
        m.register(new AutoRefill());
        m.register(new ItemCounter());
        m.register(new PotionHud());
        m.register(new FoodHud());
        m.register(new ExperienceHud());
        m.register(new DurabilityHud());

        // Visuals (original 27-40, then the visual engine list)
        m.register(new Fullbright());
        m.register(new Zoom());
        m.register(new MotionBlur());
        m.register(new CustomCrosshair());
        m.register(new BlockOverlay());
        m.register(new ItemPhysics());
        m.register(new DamageParticles());
        m.register(new TimeChanger());
        m.register(new WeatherChanger());
        m.register(new CustomSky());
        m.register(new CustomFog());
        m.register(new NameTags());
        m.register(new EntityHighlight());
        m.register(new CustomLighting());
        m.register(new WeatherVisualizer());
        m.register(new AmbientAtmosphere());
        m.register(new Bloom());
        m.register(new ColorGrading());
        m.register(new Vignette());
        m.register(new ChromaticAberration());
        m.register(new DepthEffects());
        m.register(new Sharpening());
        m.register(new FilmGrain());
        m.register(new ScreenEffects());
        m.register(new EntityInformation());
        m.register(new ItemHighlight());
        m.register(new PlayerCosmetics());
        m.register(new HealthBars());
        m.register(new ArmorDisplay());
        m.register(new CustomParticles());
        m.register(new AmbientParticles());
        m.register(new TrailEffects());
        m.register(new HitParticles());
        m.register(new CosmeticAura());
        m.register(new ExplosionVisualizer());
        m.register(new ParticlePresets());
        m.register(new Watermark());
        m.register(new ZoomVisuals());
        m.register(new CameraEffects());
        m.register(new DamageOverlay());
        m.register(new ScreenTint());
        m.register(new WaypointBeams());
        m.register(new BiomeVisualizer());
        m.register(new ChunkVisualizer());
        m.register(new CustomEnchantGlint());
        m.register(new CustomFireVisuals());
        m.register(new CustomWaterVisuals());
        m.register(new CustomGlass());
        m.register(new ThemeEngine());

        // World
        m.register(new WorldInformation());
        m.register(new Minimap());
        m.register(new Waypoints());
        m.register(new WorldMap());
        m.register(new BlockInformation());
        m.register(new LightLevel());
        m.register(new DirectionHud());
        m.register(new BiomeHud());

        // Utility
        m.register(new AutoGG());
        m.register(new ChatTimestamps());
        m.register(new ChatCustomizer());
        m.register(new AntiAfkReminder());
        m.register(new ScreenshotManager());
        m.register(new FriendList());
        m.register(new NotificationSystem());
        m.register(new AutoReconnect());
        m.register(new PerformanceMonitor());
        m.register(new PingDisplay());

        // Render & performance
        m.register(new FpsHud());
        m.register(new ChunkInformation());
        m.register(new EntityCounter());
        m.register(new ParticleControl());
        m.register(new AnimationSettings());
        m.register(new HudEditorModule());
        m.register(new ScreenshotMode());
        m.register(new DynamicResolution());
        m.register(new CustomRenderDistance());
        m.register(new EntityRendering());
        m.register(new CustomParticleQuality());
        m.register(new VisualPresets());

        // Miscellaneous
        m.register(new ModuleList());
        m.register(new Keystrokes());
        m.register(new CpsCounter());
        m.register(new ClockHud());
        m.register(new SessionStatistics());
        m.register(new ConfigurationManager());
    }
}
