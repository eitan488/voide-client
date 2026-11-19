package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String unit = "";

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        if (!(min < max) || step <= 0) throw new IllegalArgumentException("Invalid range for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public NumberSetting unit(String unit) {
        this.unit = unit;
        return this;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public String getUnit() { return unit; }
    public float getFloat() { return value.floatValue(); }
    public int getInt() { return (int) Math.round(value); }

    @Override
    protected Double sanitize(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return null;
        double clamped = Math.max(min, Math.min(max, v));
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    public String format() {
        int decimals = step >= 1 ? 0 : step >= 0.1 ? 1 : step >= 0.01 ? 2 : 3;
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value) + unit;
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
    }
}
