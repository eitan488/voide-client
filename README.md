# VOID CLIENT

A Fabric client mod for **Minecraft Java Edition 1.21.11**: a neon/AMOLED glass interface, a custom main menu and loading screen, a post-processing visual engine, 141 modules with their own settings pages, a HUD editor, JSON profiles and a keybind manager.

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.20.1 or newer |
| Fabric API | 0.141.6+1.21.11 |
| Java | 21 |
| Build | Gradle 9.2.1 (wrapper included), Fabric Loom 1.14.10, Yarn 1.21.11+build.6 |
| Mod id | `voidclient` |
| Output | `build/libs/void-client-1.0.0.jar` (a prebuilt copy is in `release/`) |

---

## 1. Build (Windows)

1. Install a **Java 21 JDK**, for example Eclipse Temurin 21. Check it in a terminal with `java -version`.
2. Unzip the project somewhere without special characters in the path, for example `C:\dev\void-client`.
3. Double-click **`build.bat`**. It runs `gradlew.bat build`. The first build downloads Gradle, Minecraft and the mappings, which takes a few minutes.
4. The finished mod is in **`build\libs\void-client-1.0.0.jar`**. The `-sources.jar` next to it is only the source code; you don't need it.

From IntelliJ IDEA, use **File → Open**, select the folder that contains `build.gradle`, and let Gradle import it. Then:

- run the Gradle task `build` to make the JAR, or
- run `runClient` (or double-click `run-client.bat`) to start a development Minecraft with the mod loaded.

From a terminal on Linux or macOS, run `./gradlew build`.

## 2. Install

1. Install **Fabric Loader 0.20.1+** for Minecraft 1.21.11 using the Fabric installer.
2. Put **Fabric API 0.141.6+1.21.11** into `.minecraft/mods`.
3. Put **`void-client-1.0.0.jar`** into `.minecraft/mods`.
4. Start the *fabric-loader-1.21.11* profile in the launcher.

On first start the client creates `.minecraft/config/voidclient/` with the eight default profiles.

## 3. Using it

| Action | Default |
|---|---|
| Open the Void GUI | **Right Shift**. Rebind it under Options → Controls → *Void Client*, or in Void Settings → Keybinds. |
| Module keybinds | Set them on each module's settings page or in Settings → Keybinds. The keybind manager supports search, conflict highlighting, mouse buttons, clearing a bind and a "reset all". |
| HUD editor | The *HUD Editor* sidebar entry, or the *HUD Editor* module. |
| Vanilla main menu | The *Vanilla Menu* button on the Void menu (top right) for one visit, or Void Settings → *Custom Main Menu* off to restore it permanently. |

Chat commands (client-side only, nothing is sent to the server):

```
/void gui
/void toggle <module name>
/void profile <profile name>
/void friend add|remove <name>   /void friend list
/void waypoint add <name>        /void waypoint goto <x> <z>
```

### Interface

- **Sidebar with 15 sections:** Dashboard, Combat, Crystal PvP, Anchor PvP, Base Hunting, Movement, Player, Visuals, World, Render, Utility, Miscellaneous, HUD Editor, Profiles, Settings.
- **Module cards:** toggle, favourite star, keybind chip, search over names and descriptions, and *Favourites* and *Recently used* rows on the Dashboard.
- **Settings panel on the right:** live preview, right-click any setting to reset it, and a *Reset* button for the whole module.
  - 11 setting types: Boolean, Number, Mode, Color (HSV picker, alpha, hex, theme-linked), Keybind (keys and mouse buttons), String, MultiSelect, Range, Position, Font and Sound.
- **Top bar:** logo, global search, active-module count, profile selector, settings and close.
- **Bottom bar:** client and game version, FPS, ping and the active profile.
- **Animation and sound:**
  - All panels use eased animations. The *Animation Settings* module (Render) controls their speed and style.
  - Ten original UI sounds (open, close, click, module on/off, notification, category switch, training hit/miss, warning).
  - Each sound setting has its own on/off, volume and pitch: the menu sounds in Void Settings, alert sounds on their modules. *UI Master Volume* scales all of them.
- **Resolution independence:**
  - The GUI scales from the framebuffer height (reference 540 px) multiplied by a user scale, so it keeps its proportions at 1080p, 1440p and 2160p and at any vanilla GUI scale.
  - HUD positions are stored as screen fractions. With *HUD Auto Scale* (Void Settings, on by default), element sizes also follow the screen height, so a layout looks the same at every resolution and GUI scale. The reference is 1080p at GUI scale 4. Each element's own *Scale* multiplies on top.
- **Main menu:**
  - Animated backgrounds (Nebula, Aurora, Starfield, Synthwave, AMOLED) with an adjustable particle count.
  - Buttons: Singleplayer, Multiplayer, Realms, Options, Client Settings and Quit.
  - A profile picker and a *Vanilla Menu* button.
  - Multiplayer opens the Void server browser: live ping, MOTD, player counts, add/edit/delete, direct connect, and a switch back to the vanilla list.
- **Loading screen:** replaces the Mojang splash with a vector progress screen.

## 4. Feature map (specification number → class)

Every module is its own class under `src/main/java/voidclient/modules/<category>/`. Where the specification lists the same feature twice (once in *Original Client Modules* 1–72 and again in *Extreme Visual Engine* 73–143), both numbers map to **one** module that implements the union of the requested settings. A second copy would only produce two toggles fighting over the same render hook.

### Original client modules 1–72

| # | Module | Class |
|---|---|---|
| 1 / 110 | Armor HUD | `combat/ArmorHud` |
| 2 | Crosshair (combat crosshair: attack-ready indicator, target colouring) | `combat/CombatCrosshair` |
| 3 / 118 | Hit Color | `combat/HitColor` |
| 4 / 94 | Damage Indicator(s) | `combat/DamageIndicator` |
| 5 / 111 | Target HUD | `combat/TargetHud` |
| 6 | Combat Information | `combat/CombatInfo` |
| 7 | Totem Counter (also a §11 combat utility) | `combat/TotemCounter` |
| 8 | Shield Information | `combat/ShieldInfo` |
| 9–17 | Sprint, Toggle Sneak, Safe Walk, Movement HUD, Coordinates (= 108), Timer Display, Step Statistics, Elytra HUD, Horse HUD | `movement/*` |
| 18–26 | Auto Tool, Inventory Manager, Inventory HUD, Auto Refill, Item Counter, Potion HUD (= 109), Food HUD, Experience HUD, Durability HUD | `player/*` |
| 27 / 73 | Fullbright | `visuals/Fullbright` |
| 28 | Zoom | `visuals/Zoom` |
| 29 / 80 | Motion Blur | `visuals/MotionBlur` |
| 30 / 116 | Custom Crosshair | `visuals/CustomCrosshair` |
| 31 / 125 | Block Overlay | `visuals/BlockOverlay` |
| 32 / 131 | Item Physics | `visuals/ItemPhysics` |
| 33 | Damage Particles | `visuals/DamageParticles` |
| 34 / 78 | Time Changer | `visuals/TimeChanger` |
| 35 | Weather Changer | `visuals/WeatherChanger` |
| 36 / 75 | Custom Sky | `visuals/CustomSky` |
| 37 / 76 | Custom Fog | `visuals/CustomFog` |
| 38 / 89 | (Custom) Name Tags | `visuals/NameTags` |
| 39 | Item ESP | `basehunting/ItemEsp` |
| 40 / 90 | Entity Highlight | `visuals/EntityHighlight` |
| 41–48 | World Information, Minimap (= 122), Waypoints, World Map (= 123), Block Information, Light Level (= 126), Direction HUD, Biome HUD | `world/*` |
| 49–58 | Auto GG, Chat Timestamps, Chat Customizer, Anti AFK Reminder, Screenshot Manager, Friend List, Notification System (= 114), Auto Reconnect, Performance Monitor, Ping Display | `utility/*` |
| 59–66 | FPS HUD (= 105), Chunk Information, Entity Counter, Particle Control, Animation Settings, HUD Editor (= 104), Screenshot Mode, Dynamic Resolution | `render/*` |
| 67–72 | Module List (= 113), Keystrokes (= 107), CPS Counter (= 106), Clock HUD, Session Statistics (= 112), Configuration Manager | `misc/*` |

### Visual engine 73–143 (entries not already listed above)

| # | Module | Class |
|---|---|---|
| 74 | Custom Lighting | `visuals/CustomLighting` |
| 77 | Weather Visualizer | `visuals/WeatherVisualizer` |
| 79 | Ambient Atmosphere | `visuals/AmbientAtmosphere` |
| 81–88 | Bloom, Color Grading, Vignette, Chromatic Aberration, Depth Effects, Sharpening, Film Grain, Screen Effects | `visuals/*` (GPU passes in `shaders/`) |
| 91, 92 | Entity Information, Item Highlight | `visuals/*` |
| 93 | Player Model Cosmetics | `visuals/PlayerCosmetics` |
| 95, 96 | Health Bars, Armor Display | `visuals/*` |
| 97–103 | Custom Particles, Ambient Particles, Trail Effects, Hit Particles, Cosmetic Aura, Explosion Visualizer, Particle Presets | `visuals/*` (engine in `particles/`) |
| 115, 117 | Watermark, Zoom Visuals | `visuals/*` |
| 119–121 | Camera Effects, Damage Overlay, Screen Tint | `visuals/*` |
| 124, 127, 128 | Waypoint Beams, Biome Visualizer, Chunk Visualizer | `visuals/*` |
| 129, 130 | Custom Render Distance, Entity Rendering | `render/*` |
| 132–135 | Custom Enchantment Glint, Custom Fire Visuals, Custom Water Visuals, Custom Glass | `visuals/*` |
| 136 | Custom Particle Quality | `render/CustomParticleQuality` |
| 137 | Visual Preset Manager | `render/VisualPresets` |
| 138–143 | Cyberpunk, Galaxy, Minimal, Plasma and AMOLED themes, plus the Custom Theme Creator | `visuals/ThemeEngine` + `gui/themes/*` |

### Base Hunting (§8)

| Part | Class |
|---|---|
| A/B Chunk Intelligence and block statistics: redstone, mechanisms, storage and farm components, with real counts from loaded chunks | `basehunting/ChunkIntelligence`, `ChunkScanner`, `ChunkStats`, `Component` |
| C Chunk Inspection ESP: borders, colour-coded categories, floating panels, icons, distance, animated outlines | `basehunting/ChunkInspectionEsp` |
| D Inspection Profiles (Storage, Redstone, Automated Farm, Item Sorter, Industrial Farm, General Structure, Custom), each with its own settings | `basehunting/InspectionProfiles` |
| E Chunk Browser: search, browse, compare, bookmarks, labels, notes, map link, CSV and report export | `basehunting/ChunkBrowser` + `gui/screens/ChunkBrowserScreen` |
| F Inspection History: coordinates, date, statistics, labels, notes, filter and search | `basehunting/InspectionHistory`, `InspectionStore` |
| G Freecam (Free / Cinematic / Orbit, cinematic HUD) | `basehunting/Freecam` |
| H Player, Entity, Item, Storage, Block, Waypoint, Friend and Custom Entity ESP | `basehunting/*Esp` |

### Crystal PvP (§9), Anchor PvP (§10), Combat utilities (§11)

| Part | Class |
|---|---|
| Crystal PvP HUD, Crystal Information | `crystal/CrystalHud`, `crystal/CrystalInfo` |
| Crystal Practice Trainer: scenarios, dummies, arena, placement preview, timing, accuracy, score, history, difficulty | `crystal/CrystalTrainer` + `training/*` |
| Crystal Combat Visuals | `crystal/CrystalVisuals` |
| Anchor HUD, Anchor Practice Trainer, Anchor Visualizer | `anchor/*` |
| Combat HUD, Armor Durability Warning, Potion Timer, Combat Session Timer, Attack Statistics, Hotbar Display, Item Information, Cooldown Information, Training Notifications | `combat/*` |
| CPS Counter, Keystrokes Display | shared with 69 and 68 (`misc/*`) |

### Systems

| System | Where |
|---|---|
| Module and settings framework, event bus | `core/`, `core/setting/`, `events/` |
| GUI: ClickGUI, pages, components, animations, themes | `gui/` |
| Main menu, server browser, loading screen | `gui/screens/MainMenuScreen`, `ServerBrowserScreen`, `LoadingScreenRenderer` |
| Visual engine: Shader, Particle, Animation, Color, Render, HUD renderer, Post-processing, Blur, Glow and Effect managers | `shaders/`, `particles/`, `gui/animations/`, `gui/themes/`, `render/`, `hud/` |
| HUD editor: drag, corner resize, X/Y, scale, guides, snap grid, layers, visibility, preview, reset | `gui/screens/HudEditorScreen` |
| Profiles: JSON; create, rename, delete, duplicate, import, export; autosave; 8 defaults | `config/ProfileManager`, `config/DefaultProfiles`, `gui/pages/ProfilesPage` |
| Keybind manager | `keybinds/`, Settings → Keybinds |
| Performance presets: Potato, Low, Balanced, High, Ultra, Cinematic | `render/VisualPresets` |

## 5. Files the client writes

Everything goes under `.minecraft/config/voidclient/`:

| Path | Content |
|---|---|
| `client.json` | Global client settings: GUI, theme, sounds, active profile, favourites |
| `profiles/*.json` | One file per profile: modules, settings, HUD layout, keybinds |
| `exports/`, `imports/` | Profile export and import folder |
| `waypoints/` | Waypoints per world or server |
| `maps/` | World-map tiles you have explored |
| `inspections/` | Chunk inspection history per world |
| `reports/` | Chunk browser CSV and report exports |
| `friends.json` | Friend list |
| `training_history.json` | Crystal and anchor trainer sessions |

`config-examples/` in this repository holds the eight default profiles exactly as the client generates them.

## 6. Limitations and the alternatives used

These are deliberate design decisions. In each case the closest legitimate behaviour is implemented instead.

- **No automated combat.** Nothing in the client attacks, places crystals, detonates anchors or aims for you:
  - the trainers only measure your own inputs;
  - Hit Color, the target indicators and the damage numbers are visual only;
  - Auto Tool, Auto Refill and Inventory Manager only touch your own inventory and never act on other players.
- **Base Hunting reads only client-visible data.**
  - Chunk statistics come from chunks the server already sent to your client.
  - Nothing is guessed about unloaded chunks.
  - There is no x-ray of ores and no packet tricks.
- **Freecam is purely client-side.**
  - Your real player gets no movement or look input, so its server-side position never changes.
  - *Allowed In* defaults to singleplayer and spectator mode.
  - Interaction from the detached camera is blocked unless you enable *Allow Interaction*.
- **Trainer dummies and arena are singleplayer only**, because they spawn entities and blocks in an integrated-server world. On a multiplayer server the trainers run in tracking-only mode.
- **Damage numbers are estimates.** They are derived from health changes the client can see. The server's real damage values are not available to the client.
- **Dynamic Resolution** adapts render distance and effect detail to keep a target FPS. It does not render the world at a lower internal resolution, because that would mean replacing Minecraft's main framebuffer pipeline.
- **Custom Glass** ships two built-in resource packs (Clear and Borderless) that are enabled with a resource reload. **Custom Enchantment Glint** swaps and tints the glint texture.
- **Cosmetics** (Player Model Cosmetics, Cosmetic Aura, trails) are only visible to you.
- **Fonts:** Inter (SIL OFL 1.1) and JetBrains Mono (SIL OFL 1.1) are bundled; see `licenses/`. Minecraft only accepts TrueType outlines, so Inter was converted from CFF to TTF.

## 7. Test status

**Tested in this repository's build environment:**
- `gradlew build` succeeds.
- The client was run headless on Linux with Mesa llvmpipe (software OpenGL), driven by an automated in-game self-test.
  - **Full pass at 1920×1080:**
    - checks that every mixin applied;
    - opens every GUI page, the settings panel, the server browser, the HUD editor, the world map and the chunk browser;
    - creates a world, applies all eight default profiles and checks each HUD layout for overlapping elements;
    - runs the crystal trainer and freecam;
    - enables 125 modules at once and checks that none were disabled by the error handler.
  - The full pass ran twice: once in the dev environment (`runSelftest`) and once against the remapped release JAR in a production-style Fabric install with real Fabric Loader 0.20.1 and Fabric API 0.141.6 (`prodSelftest`).
  - **Short pass at 2560×1440 and 3840×2160:** menu, GUI, world, default/Performance/Crystal profiles and HUD editor.
  - The final passes (release JAR at 1080p, short passes at 1440p and 2160p) ended with all mixins applied, no module disabled by an error, and 0 HUD overlaps in every profile tested.

**Not tested:**
- Windows, a real GPU, multiplayer servers and audio playback (the test machine has no audio device).
- Frame rates on real hardware: software rendering runs at a few FPS, so performance figures from the test machine mean nothing.

If anything misbehaves, `logs/latest.log` lines tagged `(Void Client)` show which module failed. A module that throws three times in a row disables itself and shows a notification.

## 8. Development

```bash
./gradlew build            # JAR in build/libs/
./gradlew runClient        # dev client
./gradlew runSelftest      # automated in-game smoke test, dev mappings (writes run/voidclient-selftest.txt)
./gradlew prodSelftest     # same test against the remapped release JAR in a production-style install (run-prod/)
```

Source layout follows the specification: `voidclient/{core, modules/<category>, gui/{screens,components,animations,themes,settings}, render, shaders, particles, hud, config, keybinds, events, utilities}` and `resources/assets/voidclient/{textures, sounds, font, shaders, lang}`.

## License

Void Client code and assets: all rights reserved by the project owner. Bundled fonts: see `licenses/`.
