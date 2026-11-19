package voidclient.core.setting;

/** Global dirty flag so the config manager can autosave only when something changed. */
public final class SettingChangeTracker {
    private static volatile boolean dirty;

    private SettingChangeTracker() {}

    public static void markDirty() { dirty = true; }

    public static boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }
}
