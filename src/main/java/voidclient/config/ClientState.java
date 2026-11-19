package voidclient.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.core.setting.SettingChangeTracker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Favourites, recently used modules and the active profile name. */
public final class ClientState {
    private final Set<String> favorites = new LinkedHashSet<>();
    private final List<String> recent = new ArrayList<>();
    private String activeProfile = "Default";
    private boolean firstRun = true;

    public boolean isFavorite(String module) { return favorites.contains(module); }

    public void toggleFavorite(String module) {
        if (!favorites.remove(module)) favorites.add(module);
        SettingChangeTracker.markDirty();
    }

    public Set<String> favorites() { return favorites; }
    public List<String> recent() { return recent; }

    public void markRecent(String module) {
        recent.remove(module);
        recent.add(0, module);
        while (recent.size() > 12) recent.remove(recent.size() - 1);
    }

    public String activeProfile() { return activeProfile; }
    public void setActiveProfile(String p) { activeProfile = p; }
    public boolean isFirstRun() { return firstRun; }
    public void setFirstRun(boolean f) { firstRun = f; }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        JsonArray f = new JsonArray();
        favorites.forEach(f::add);
        JsonArray r = new JsonArray();
        recent.forEach(r::add);
        o.add("favorites", f);
        o.add("recent", r);
        o.addProperty("activeProfile", activeProfile);
        o.addProperty("firstRun", firstRun);
        return o;
    }

    public void load(JsonObject o) {
        if (o == null) return;
        favorites.clear();
        recent.clear();
        if (o.get("favorites") instanceof JsonArray a) for (JsonElement e : a) favorites.add(e.getAsString());
        if (o.get("recent") instanceof JsonArray a) for (JsonElement e : a) recent.add(e.getAsString());
        if (o.has("activeProfile")) activeProfile = o.get("activeProfile").getAsString();
        if (o.has("firstRun")) firstRun = o.get("firstRun").getAsBoolean();
    }
}
