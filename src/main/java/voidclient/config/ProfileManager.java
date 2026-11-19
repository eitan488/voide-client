package voidclient.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import voidclient.VoidClient;
import voidclient.core.Module;
import voidclient.core.ModuleManager;
import voidclient.gui.Notifications;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * JSON profiles in config/voidclient/profiles. A profile stores every module's enabled state,
 * keybind and settings (HUD layout, theme and keybinds live in module settings, so they are
 * all captured per profile). Applying a profile resets modules first, so missing keys fall
 * back to defaults.
 */
public final class ProfileManager {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9 _\\-]{1,32}");
    private final ConfigManager config;

    public ProfileManager(ConfigManager config) { this.config = config; }

    public Path profilesDir() { return config.dir().resolve("profiles"); }
    public Path exportsDir() { return config.dir().resolve("exports"); }
    public Path importsDir() { return config.dir().resolve("imports"); }

    private Path path(String name) { return profilesDir().resolve(name + ".json"); }

    public static boolean isValidName(String name) { return name != null && VALID.matcher(name).matches() && !name.isBlank(); }

    public String active() { return VoidClient.get().clientState().activeProfile(); }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(profilesDir());
            try (Stream<Path> s = Files.list(profilesDir())) {
                s.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(out::add);
            }
        } catch (IOException e) {
            VoidClient.LOG.error("Could not list profiles", e);
        }
        return out;
    }

    public boolean exists(String name) { return Files.isRegularFile(path(name)); }

    /** First start: write the default profiles, then load the active one. */
    public void init() {
        if (list().isEmpty()) {
            for (String name : DefaultProfiles.NAMES) JsonFiles.write(path(name), DefaultProfiles.build(name));
        }
        String active = active();
        if (!exists(active)) active = list().isEmpty() ? "Default" : list().get(0);
        load(active, false);
    }

    public JsonObject snapshot(String name) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("name", name);
        root.addProperty("client", VoidClient.VERSION);
        JsonObject modules = new JsonObject();
        for (Module m : VoidClient.get().modules().all()) modules.add(m.getName(), m.save());
        root.add("modules", modules);
        return root;
    }

    public void saveActive() { JsonFiles.write(path(active()), snapshot(active())); }

    public boolean load(String name, boolean notify) {
        JsonObject root = JsonFiles.read(path(name));
        if (root == null) {
            if (notify) Notifications.error("Profile", "Could not read " + name);
            return false;
        }
        apply(root);
        VoidClient.get().clientState().setActiveProfile(name);
        if (notify) Notifications.success("Profile loaded", name);
        return true;
    }

    /** Saves the current profile, then switches. */
    public void switchTo(String name) {
        if (name.equals(active())) return;
        saveActive();
        load(name, true);
        config.saveClient();
    }

    private void apply(JsonObject root) {
        ModuleManager mm = VoidClient.get().modules();
        mm.setLoading(true);
        try {
            JsonObject modules = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : mm.all()) {
                m.resetSettings();
                JsonElement e = modules.get(m.getName());
                if (e != null && e.isJsonObject()) {
                    m.load(e.getAsJsonObject());
                } else {
                    m.setEnabled(false);
                }
            }
        } finally {
            mm.setLoading(false);
        }
    }

    public boolean create(String name, boolean fromCurrent) {
        if (!isValidName(name) || exists(name)) return false;
        JsonObject json = fromCurrent ? snapshot(name) : DefaultProfiles.build("Default");
        json.addProperty("name", name);
        return JsonFiles.write(path(name), json);
    }

    public boolean duplicate(String source, String target) {
        if (!isValidName(target) || exists(target)) return false;
        if (source.equals(active())) saveActive();
        JsonObject json = JsonFiles.read(path(source));
        if (json == null) return false;
        json.addProperty("name", target);
        return JsonFiles.write(path(target), json);
    }

    public boolean rename(String from, String to) {
        if (!isValidName(to) || exists(to) || !exists(from)) return false;
        if (from.equals(active())) saveActive();
        try {
            Files.move(path(from), path(to));
        } catch (IOException e) {
            return false;
        }
        if (from.equals(active())) {
            VoidClient.get().clientState().setActiveProfile(to);
            config.saveClient();
        }
        return true;
    }

    public boolean delete(String name) {
        List<String> all = list();
        if (all.size() <= 1 || !exists(name)) return false;
        try {
            Files.deleteIfExists(path(name));
        } catch (IOException e) {
            return false;
        }
        if (name.equals(active())) {
            List<String> rest = list();
            load(rest.get(0), true);
            config.saveClient();
        }
        return true;
    }

    /** Writes the profile to exports/ and puts its JSON on the clipboard. Returns the file. */
    public Path export(String name) {
        if (name.equals(active())) saveActive();
        JsonObject json = JsonFiles.read(path(name));
        if (json == null) return null;
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = exportsDir().resolve(name + "-" + stamp + ".json");
        if (!JsonFiles.write(out, json)) return null;
        MinecraftClient.getInstance().keyboard.setClipboard(JsonFiles.GSON.toJson(json));
        return out;
    }

    /** Imports a profile from clipboard JSON. Returns the new profile name or null. */
    public String importFromClipboard() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        try {
            JsonElement e = JsonParser.parseString(clip);
            if (!e.isJsonObject() || !e.getAsJsonObject().has("modules")) return null;
            return importJson(e.getAsJsonObject());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Imports every *.json in config/voidclient/imports. Returns how many were imported. */
    public int importFolder() {
        int n = 0;
        try {
            Files.createDirectories(importsDir());
            try (Stream<Path> s = Files.list(importsDir())) {
                for (Path p : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                    JsonObject json = JsonFiles.read(p);
                    if (json != null && json.has("modules") && importJson(json) != null) {
                        n++;
                        Files.move(p, p.resolveSibling(p.getFileName() + ".imported"));
                    }
                }
            }
        } catch (IOException e) {
            VoidClient.LOG.error("Import failed", e);
        }
        return n;
    }

    private String importJson(JsonObject json) {
        String base = json.has("name") ? json.get("name").getAsString().replaceAll("[^A-Za-z0-9 _\\-]", "") : "Imported";
        if (base.isBlank()) base = "Imported";
        if (base.length() > 26) base = base.substring(0, 26);
        String name = base;
        int i = 2;
        while (exists(name)) name = base + " " + i++;
        json.addProperty("name", name);
        return JsonFiles.write(path(name), json) ? name : null;
    }
}
