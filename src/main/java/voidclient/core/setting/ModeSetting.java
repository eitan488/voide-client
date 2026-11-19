package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue)) throw new IllegalArgumentException("Default not in modes for " + name);
    }

    public List<String> getModes() { return modes; }
    public boolean is(String mode) { return value.equals(mode); }
    public int index() { return modes.indexOf(value); }

    public void cycle(boolean forward) {
        int i = index() + (forward ? 1 : -1);
        set(modes.get(Math.floorMod(i, modes.size())));
    }

    @Override
    protected String sanitize(String v) { return modes.contains(v) ? v : null; }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) set(e.getAsString());
    }
}
