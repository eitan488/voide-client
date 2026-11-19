package voidclient.core;

import voidclient.VoidClient;
import voidclient.gui.Notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> byName = new LinkedHashMap<>();
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private boolean loading;

    public void register(Module m) {
        String key = m.getName().toLowerCase(Locale.ROOT);
        if (byName.containsKey(key)) throw new IllegalStateException("Duplicate module name " + m.getName());
        modules.add(m);
        byName.put(key, m);
        byClass.put(m.getClass(), m);
    }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    public Optional<Module> byName(String name) { return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT))); }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        Module m = byClass.get(type);
        if (m == null) throw new IllegalArgumentException("Module not registered: " + type.getSimpleName());
        return (T) m;
    }

    public List<Module> inCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.isListedIn(c)) out.add(m);
        return out;
    }

    public List<HudModule> hudModules() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : modules) if (m instanceof HudModule h) out.add(h);
        return out;
    }

    public long enabledCount() { return modules.stream().filter(Module::isEnabled).count(); }

    /** Suppresses sounds/notifications while a profile is being applied. */
    public void setLoading(boolean loading) { this.loading = loading; }

    void onModuleToggled(Module m) {
        if (loading) return;
        VoidClient.get().clientState().markRecent(m.getName());
    }

    /** Toggle requested by the user (keybind / GUI): plays feedback. */
    public void userToggle(Module m, boolean fromKeybind) {
        m.toggle();
        if (!m.isToggleable()) return;
        var settings = VoidClient.get().clientSettings();
        (m.isEnabled() ? settings.moduleOnSound : settings.moduleOffSound).play();
        if (fromKeybind && m.toggleNotification.get()) {
            Notifications.info(m.getName(), m.isEnabled() ? "Enabled" : "Disabled");
        }
    }

    public static void playClick() {
        VoidClient.get().clientSettings().clickSound.play();
    }

}
