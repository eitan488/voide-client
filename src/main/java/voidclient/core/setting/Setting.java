package voidclient.core.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class of every module setting. A setting owns its default value, current value,
 * JSON (de)serialisation and change listeners.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private boolean hidden;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = copy(defaultValue);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    public T getDefault() { return defaultValue; }

    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (sanitized == null) return;
        boolean changed = !sanitized.equals(value);
        value = sanitized;
        if (changed) {
            for (Consumer<T> l : listeners) l.accept(value);
            SettingChangeTracker.markDirty();
        }
    }

    public void reset() { set(copy(defaultValue)); }

    /** Changes the default during module construction (also sets the current value). */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S defaultTo(T newDefault) {
        this.defaultValue = newDefault;
        this.value = copy(newDefault);
        return (S) this;
    }

    public boolean isDefault() { return value.equals(defaultValue); }

    /** Lets subclasses clamp or validate. Return null to reject the value. */
    protected T sanitize(T v) { return v; }

    /** Deep copy for mutable value types (overridden by collection settings). */
    protected T copy(T v) { return v; }

    public abstract JsonElement toJson();

    /** Must tolerate malformed input: on failure keep the current value. */
    public abstract void fromJson(JsonElement element);

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> listener) {
        listeners.add(listener);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S hide() {
        this.hidden = true;
        return (S) this;
    }

    public boolean isVisible() { return !hidden && visibility.getAsBoolean(); }
    public boolean isHidden() { return hidden; }
}
