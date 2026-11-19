package voidclient.config;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import voidclient.VoidClient;
import voidclient.core.setting.SettingChangeTracker;

import java.nio.file.Path;

/** Owns the config directory, client.json and the autosave loop. */
public final class ConfigManager {
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("voidclient");
    private long lastSave = System.currentTimeMillis();

    public Path dir() { return dir; }
    public Path file(String name) { return dir.resolve(name); }

    public void loadClient() {
        JsonObject o = JsonFiles.read(file("client.json"));
        if (o == null) return;
        VoidClient.get().clientSettings().load(o.has("settings") ? o.getAsJsonObject("settings") : null);
        VoidClient.get().clientState().load(o.has("state") ? o.getAsJsonObject("state") : null);
    }

    public void saveClient() {
        JsonObject o = new JsonObject();
        o.addProperty("version", 1);
        o.add("settings", VoidClient.get().clientSettings().save());
        o.add("state", VoidClient.get().clientState().save());
        JsonFiles.write(file("client.json"), o);
    }

    public void saveAll() {
        saveClient();
        VoidClient.get().profiles().saveActive();
        lastSave = System.currentTimeMillis();
    }

    /** Called every client tick; saves only when settings changed and the interval passed. */
    public void tick() {
        long interval = (long) (VoidClient.get().clientSettings().autosaveSeconds.get() * 1000);
        if (System.currentTimeMillis() - lastSave < interval) return;
        lastSave = System.currentTimeMillis();
        if (SettingChangeTracker.consumeDirty()) saveAll();
    }
}
