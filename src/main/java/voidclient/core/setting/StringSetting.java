package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
    }

    public int getMaxLength() { return maxLength; }

    @Override
    protected String sanitize(String v) {
        if (v == null) return null;
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) set(e.getAsString());
    }
}
