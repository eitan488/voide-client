package voidclient.core.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/** A [low, high] pair constrained inside [min, max]. */
public class RangeSetting extends Setting<RangeSetting.Range> {
    public record Range(double low, double high) {}

    private final double min;
    private final double max;
    private final double step;

    public RangeSetting(String name, String description, double low, double high, double min, double max, double step) {
        super(name, description, new Range(low, high));
        if (!(min < max) || step <= 0) throw new IllegalArgumentException("Invalid range for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public double low() { return value.low(); }
    public double high() { return value.high(); }

    public boolean contains(double v) { return v >= value.low() && v <= value.high(); }

    private double snap(double v) {
        double c = Math.max(min, Math.min(max, v));
        return Math.max(min, Math.min(max, min + Math.round((c - min) / step) * step));
    }

    @Override
    protected Range sanitize(Range r) {
        if (r == null || Double.isNaN(r.low()) || Double.isNaN(r.high())) return null;
        double lo = snap(Math.min(r.low(), r.high()));
        double hi = snap(Math.max(r.low(), r.high()));
        return new Range(lo, hi);
    }

    @Override
    public JsonElement toJson() {
        JsonArray a = new JsonArray();
        a.add(value.low());
        a.add(value.high());
        return a;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (e instanceof JsonArray a && a.size() == 2) {
            try {
                set(new Range(a.get(0).getAsDouble(), a.get(1).getAsDouble()));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
