package voidclient.config;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * The eight built-in profiles as sparse JSON: listed modules are enabled (optionally with
 * setting overrides); everything else stays at defaults/disabled when a profile is applied.
 */
public final class DefaultProfiles {
    public static final List<String> NAMES = List.of("Default", "Performance", "PvP Practice", "Crystal Practice", "Anchor Practice",
            "Base Inspection", "Visual Showcase", "Custom");

    private DefaultProfiles() {}

    private static final List<String> BASE = List.of("Watermark", "Module List", "FPS HUD", "Coordinates", "Notification System", "Sprint");

    private static List<String> modules(String profile) {
        return switch (profile) {
            case "Performance" -> List.of("Watermark", "FPS HUD", "Ping Display", "Notification System", "Particle Control", "Custom Particle Quality",
                    "Entity Rendering", "Dynamic Resolution", "Performance Monitor", "Sprint");
            case "PvP Practice" -> concat(BASE, List.of("Keystrokes", "CPS Counter", "Target HUD", "Armor HUD", "Crosshair", "Combat Information",
                    "Attack Statistics", "Potion HUD", "Totem Counter", "Hit Color", "Damage Indicator", "Camera Effects", "Combat Session Timer"));
            case "Crystal Practice" -> concat(BASE, List.of("Crystal PvP HUD", "Crystal Information", "Crystal Combat Visuals", "Explosion Visualizer",
                    "Totem Counter", "Target HUD", "Training Notifications", "Armor Durability Warning", "Keystrokes", "CPS Counter"));
            case "Anchor Practice" -> concat(BASE, List.of("Anchor HUD", "Anchor Visualizer", "Totem Counter", "Target HUD", "Training Notifications",
                    "Armor Durability Warning", "Item Counter"));
            case "Base Inspection" -> concat(BASE, List.of("Chunk Intelligence", "Chunk Inspection ESP", "Storage ESP", "Minimap", "Waypoints",
                    "Waypoint Beams", "Chunk Visualizer", "Fullbright", "Direction HUD", "Biome HUD"));
            case "Visual Showcase" -> concat(BASE, List.of("Bloom", "Color Grading", "Vignette", "Film Grain", "Ambient Particles", "Trail Effects",
                    "Cosmetic Aura", "Player Model Cosmetics", "Custom Sky", "Block Overlay", "Item Physics", "Custom Crosshair", "Hit Particles",
                    "Damage Overlay", "Ambient Atmosphere"));
            default -> concat(BASE, List.of("Keystrokes", "CPS Counter", "Potion HUD", "Armor HUD", "Minimap", "Waypoints", "Block Overlay",
                    "Zoom Visuals", "Chat Timestamps", "Chat Customizer"));
        };
    }

    private static List<String> concat(List<String> a, List<String> b) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(a);
        for (String s : b) if (!out.contains(s)) out.add(s);
        return out;
    }

    /** Per-profile setting overrides: module -> (setting -> JSON value). */
    private static Map<String, Map<String, Object>> overrides(String profile) {
        return switch (profile) {
            case "Performance" -> Map.of("Theme Engine", Map.of("Theme", "Minimal", "Glow Strength", 0.4),
                    "Custom Particle Quality", Map.of("Vanilla Density", 30.0, "Block Break Density", 15.0),
                    "Performance Monitor", Map.of("Position", pos(0.005, 0.26)));
            // The crystal panel takes the left column, so keystrokes move beside the coordinates.
            case "Crystal Practice" -> Map.of("Theme Engine", Map.of("Theme", "Plasma"), "Keystrokes", Map.of("Position", pos(0.22, 0.085)));
            // The anchor panel takes the left column; the item counter becomes a bar along the top.
            case "Anchor Practice" -> Map.of("Theme Engine", Map.of("Theme", "Plasma"),
                    "Item Counter", Map.of("Position", pos(0.36, 0.005), "Layout", "Horizontal"));
            case "Base Inspection" -> Map.of("Theme Engine", Map.of("Theme", "Galaxy"), "Inspection Profiles", Map.of("Active Profile", "General Structure Inspection"));
            case "Visual Showcase" -> Map.of("Theme Engine", Map.of("Theme", "Cyberpunk", "Glow Strength", 1.4), "Color Grading", Map.of("Preset", "Cyberpunk"));
            case "PvP Practice" -> Map.of("Theme Engine", Map.of("Theme", "AMOLED"));
            default -> Map.of();
        };
    }

    private static double[] pos(double x, double y) {
        return new double[]{x, y};
    }

    public static JsonObject build(String profile) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("name", profile);
        JsonObject mods = new JsonObject();
        for (String m : modules(profile)) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", true);
            mods.add(m, o);
        }
        for (var e : overrides(profile).entrySet()) {
            JsonObject o = mods.has(e.getKey()) ? mods.getAsJsonObject(e.getKey()) : new JsonObject();
            JsonObject settings = new JsonObject();
            for (var s : e.getValue().entrySet()) {
                Object v = s.getValue();
                if (v instanceof double[] xy) {
                    JsonObject at = new JsonObject();
                    at.addProperty("x", xy[0]);
                    at.addProperty("y", xy[1]);
                    settings.add(s.getKey(), at);
                } else if (v instanceof Number n) settings.addProperty(s.getKey(), n);
                else settings.addProperty(s.getKey(), String.valueOf(v));
            }
            o.add("settings", settings);
            mods.add(e.getKey(), o);
        }
        root.add("modules", mods);
        return root;
    }
}
