package voidclient.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import voidclient.VoidClient;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.KeybindSetting;
import voidclient.core.setting.Setting;
import voidclient.core.setting.SettingChangeTracker;
import voidclient.events.EventBus;
import voidclient.gui.Notifications;
import voidclient.keybinds.Keybind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base class for every feature. Event listeners declared with {@link #on} are only registered
 * while the module is enabled, so disabled modules cost nothing per frame.
 */
public abstract class Module implements EventBus.ErrorHandler {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final Set<Category> alsoListedIn = EnumSet.noneOf(Category.class);
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventBus.Listener<?>> listeners = new ArrayList<>();
    private boolean enabled;
    private int errorCount;

    public final KeybindSetting keybind;
    public final BooleanSetting toggleNotification;
    public final BooleanSetting showInModuleList;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = add(new KeybindSetting("Keybind", "Key or mouse button that toggles this module.", Keybind.NONE));
        this.toggleNotification = add(new BooleanSetting("Toggle Notification", "Show a notification when this module is toggled by keybind.", true));
        this.showInModuleList = add(new BooleanSetting("Show In Module List", "List this module in the Module List HUD while enabled.", true));
    }

    // ---- declaration helpers -------------------------------------------------------------

    protected final <S extends Setting<?>> S add(S setting) {
        for (Setting<?> s : settings) {
            if (s.getName().equals(setting.getName())) throw new IllegalStateException("Duplicate setting " + setting.getName() + " in " + name);
        }
        settings.add(setting);
        return setting;
    }

    protected final <E> void on(Class<E> type, Consumer<E> handler) { on(type, 0, handler); }

    protected final <E> void on(Class<E> type, int priority, Consumer<E> handler) {
        listeners.add(VoidClient.get().events().create(type, handler, priority, this));
    }

    protected final void alsoListIn(Category... categories) { Collections.addAll(alsoListedIn, categories); }

    // ---- lifecycle --------------------------------------------------------------------------

    public final void toggle() { setEnabled(!enabled); }

    public final void setEnabled(boolean value) {
        if (!isToggleable()) {
            if (value) runAction();
            return;
        }
        if (value == enabled) return;
        enabled = value;
        EventBus bus = VoidClient.get().events();
        if (value) {
            errorCount = 0;
            listeners.forEach(bus::register);
            safe(this::onEnable);
        } else {
            listeners.forEach(bus::unregister);
            safe(this::onDisable);
        }
        SettingChangeTracker.markDirty();
        VoidClient.get().modules().onModuleToggled(this);
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            VoidClient.LOG.error("Module {} failed during toggle", name, t);
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Modules that are actions (e.g. open a screen) instead of on/off features. */
    public boolean isToggleable() { return true; }

    protected void runAction() {}

    @Override
    public void onListenerError(Throwable t) {
        VoidClient.LOG.error("Module {} threw in an event handler", name, t);
        if (++errorCount >= 3 && enabled) {
            setEnabled(false);
            Notifications.error(name + " disabled", "Repeated error: " + t.getClass().getSimpleName());
        }
    }

    // ---- queries ------------------------------------------------------------------------

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isListedIn(Category c) { return category == c || alsoListedIn.contains(c); }
    public boolean isEnabled() { return enabled; }
    public List<Setting<?>> getSettings() { return settings; }

    /** Extra text shown next to the name in the module list (mode, value...). */
    public String getInfo() { return null; }

    /** Modules can draw a live preview in the settings panel. */
    public boolean hasPreview() { return false; }

    public void renderPreview(DrawContext ctx, float x, float y, float w, float h, float delta) {}

    protected static boolean inWorld() { return mc.player != null && mc.world != null; }

    // ---- persistence --------------------------------------------------------------------

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        JsonObject s = new JsonObject();
        for (Setting<?> setting : settings) s.add(setting.getName(), setting.toJson());
        o.add("settings", s);
        return o;
    }

    public void load(JsonObject o) {
        if (o == null) return;
        JsonObject s = o.has("settings") && o.get("settings").isJsonObject() ? o.getAsJsonObject("settings") : null;
        if (s != null) {
            for (Setting<?> setting : settings) {
                JsonElement e = s.get(setting.getName());
                if (e != null) setting.fromJson(e);
            }
        }
        if (o.has("enabled") && isToggleable()) {
            try {
                setEnabled(o.get("enabled").getAsBoolean());
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void resetSettings() {
        for (Setting<?> s : settings) s.reset();
    }
}
