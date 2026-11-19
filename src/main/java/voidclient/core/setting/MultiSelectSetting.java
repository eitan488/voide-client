package voidclient.core.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiSelectSetting extends Setting<Set<String>> {
    private final List<String> options;

    public MultiSelectSetting(String name, String description, List<String> options, Set<String> defaults) {
        super(name, description, Collections.unmodifiableSet(new LinkedHashSet<>(defaults)));
        this.options = List.copyOf(options);
    }

    public List<String> getOptions() { return options; }
    public boolean isSelected(String option) { return value.contains(option); }

    public void toggle(String option) {
        if (!options.contains(option)) return;
        Set<String> next = new LinkedHashSet<>(value);
        if (!next.remove(option)) next.add(option);
        set(next);
    }

    public void setSelected(String option, boolean selected) {
        if (isSelected(option) != selected) toggle(option);
    }

    @Override
    protected Set<String> sanitize(Set<String> v) {
        if (v == null) return null;
        Set<String> out = new LinkedHashSet<>();
        for (String o : options) if (v.contains(o)) out.add(o);
        return Collections.unmodifiableSet(out);
    }

    @Override
    protected Set<String> copy(Set<String> v) { return Collections.unmodifiableSet(new LinkedHashSet<>(v)); }

    @Override
    public JsonElement toJson() {
        JsonArray a = new JsonArray();
        value.forEach(a::add);
        return a;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (!(e instanceof JsonArray a)) return;
        Set<String> s = new LinkedHashSet<>();
        for (JsonElement x : a) if (x.isJsonPrimitive()) s.add(x.getAsString());
        set(s);
    }
}
