package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import voidclient.keybinds.Keybind;

public class KeybindSetting extends Setting<Keybind> {
    public KeybindSetting(String name, String description, Keybind defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value.serialize()); }

    @Override
    public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) set(Keybind.parse(e.getAsString()));
    }
}
